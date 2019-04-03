#include"user.h"
#include"MySQLManager.h"

MySQLManager *Mysql;

//�ļ���Ϊk��
int ramp_k;

//����m��������
int ramp_m;

//word size
int ramp_w;

int blocksize;
int packetsize;


int *matrix;
int *bitmatrix;
int **schedule;

map<string, User *> all_users;

User::User(char *name,char *password,SSL * clntSock)
{
	userid = new char[33];
	for (int i = 0; i < 32; i++)
	{
		userid[i] = name[i];
	}
	userid[32]=0;
	this->clntSock = clntSock;
	userpassword = new char[33];
	for (int i = 0; i < 32; i++)
	{
		userpassword[i] = password[i];
	}
	userpassword[32] = 0;
}

User::~User()
{
	delete[] userid;
	delete[] userpassword;
}

void  User::handleClnt()
{
	char buffer[1];

	//�յ����ֽ���
	int readnum = 0;
	int flag = 1;


	while (flag)
	{
		//printf("���߳�%d\n", GetCurrentThreadId());
		//printf("�ȴ�");
		readnum = SSL_read(clntSock, buffer, 1);
		if (readnum <= 0)
		{
			printf("�û��ѶϿ�����!\n");
			sign_out();
			flag = 0;
			return;
		}
		else
		{
			switch (buffer[0]){
			case SIGN_OUT:
				//����жϵ�Ŀ����Ҫ���ж����ݿ���û�г���
				if (sign_out())
				{
					printf("һ�û����˳�...\n");
					//һ���˳��ͷ���
					return;
				}
				break;
			case REQUIRE_FILES:
				//�������ݿ���ִ��󣬷��������ļ��б�һ���ɹ�
				require_files();
				printf("һ�û���ȡ���ļ��б�\n");
				break;
			case REQUIRE_FRIENDS:
				require_friends();			
				printf("һ�û�ȡ�ú����б�ɹ���\n");
				break;
			case ADD_FRIEND:
				if(add_friend())
					printf("һ�û������һ���ѣ�\n");
				else
					printf("һ�û���Ӻ���ʧ�ܣ�\n");
				break;
			case DELETE_FILE:
				if (deleteFile())
					printf("һ�û�ɾ���ļ��ɹ���\n");
				else
					printf("һ�û�ɾ���ļ�ʧ�ܣ�\n");
				break;
			case CREATE_DIR:
				if (createDir())
					printf("һ�û��Ѵ���Ŀ¼\n");
				else
					printf("һ�û�����Ŀ¼ʧ��\n");
				break;
			case SHARE_FILE:
				if (share_file())
				{
					printf("һ�û������ļ��ɹ���\n");
				}
				else
				{
					printf("һ�û������ļ�ʧ�ܣ�\n");
				}
				break;
			default:
				flag = 0;
				break;
			}
		}
	}
	printf("�û����Ϸ�����Ϊ�������ӱ������رգ�\n");
}



int User::sign_out()
{
	return Mysql->personLogout(userid);
}


//��������֡��ʽ����־��1��|�ļ�������4��|id��4��|�ļ�id��32��|�ļ������ȣ�1��|�ļ�����*��|�ļ���С��4��|�ļ���Կ��129��|�ļ����ͣ�1��|*
//�û������ļ��б�
bool User::require_files()
{
	char *buffer=NULL;
	int readnum = 0;

	unsigned char *sint = new unsigned char[4];
	readnum = SSL_read(clntSock, (char *)sint, 4);
	while (readnum != 4)
	{
		if (readnum <= 0)
		{
			delete[] sint;
			return false;
		}
		readnum += SSL_read(clntSock, (char *)sint + readnum, 4 - readnum);
	}
	int id = bytes_to_int(sint);
	readnum = SSL_read(clntSock, (char *)sint, 4);
	while (readnum != 4)
	{
		if (readnum <= 0)
		{
			delete[] sint;
			return false;
		}
		readnum += SSL_read(clntSock, (char *)sint + readnum, 4 - readnum);
	}
	int deep = bytes_to_int(sint);
	int sendnum = Mysql->getFiles(userid, id, deep, &buffer);
	
	SSL_write(clntSock, buffer, sendnum);
	delete[] buffer;
	delete[] sint;
	return true;
}

//��Ӧ����û�з���ֵ��
int User::require_friends()
{
	char *buffer=NULL;
	int len = Mysql->getFriends(userid, &buffer);
	
	SSL_write(clntSock, buffer, len);
	delete[] buffer;
	return 0;
}

//�û���Ӻ��ѣ�ֻ��Ҫ���͸ú��ѵ����ֵĹ�ϣ�Ϳ����ˣ��᷵�ظ��û����ѵĹ�Կ
int User::add_friend()
{
	char *buffer=NULL;
	char *namehash = new char[33];
	memset(namehash, 0, 33*sizeof(char));
	int readnum = SSL_read(clntSock, namehash, 32);
	while (readnum != 32)
	{
		if (readnum <= 0)
		{
			return 0;
		}
		readnum += SSL_read(clntSock, namehash + readnum, 32 - readnum);
	}
	bool b = Mysql->getPerson(namehash, &buffer);
	if (!b)
	{
		SSL_write(clntSock, buffer, 1);
		delete[] namehash;
		delete[] buffer;
		return 0;
	}
	SSL_write(clntSock, buffer, 66);
	Mysql->friendInsert(userid, namehash);
	Mysql->friendInsert(namehash, userid);
	delete[] namehash;
	delete[] buffer;
	return 1;

}




int User::createDir()
{
	int readnum;
	unsigned char sint[4];
	readnum = SSL_read(clntSock, (char *)sint, 1);
	
	int namelen = sint[0];
	char *name = new char[namelen+1];
	readnum = SSL_read(clntSock, name, namelen);
	while (readnum != namelen)
	{
		if (readnum <= 0)
		{
			delete[] name;
			return false;
		}
		readnum += SSL_read(clntSock, name + readnum, namelen - readnum);
	}
	name[namelen] = 0;
	readnum = SSL_read(clntSock, (char *)sint, 4);
	while (readnum != 4)
	{
		if (readnum <= 0)
		{
			delete[] name;
			return false;
		}
		readnum += SSL_read(clntSock, (char *)sint + readnum, 4 - readnum);
	}
	int id = bytes_to_int(sint);

	readnum = SSL_read(clntSock, (char *)sint, 4);
	while (readnum != 4)
	{
		if (readnum <= 0)
		{
			delete[] name;
			return false;
		}
		readnum += SSL_read(clntSock, (char *)sint + readnum, 4 - readnum);
	}
	int deep = bytes_to_int(sint);
	char temp1[33];
	memset(temp1, 0xff, 32);
	temp1[32] = 0;
	char temp2[130];
	memset(temp2, 0xff, 129);
	temp2[129] = 0;
	int tid = Mysql->writeFile(userid, name, namelen, 0, temp1, temp2, id, deep, 0);
	if (tid!=0)
	{
		delete[] name;
		char s[5];
		s[0] = SUCCESS_CREATE;
		int_to_bytes(tid, (unsigned char *)s + 1);
		SSL_write(clntSock, s, 5);
		return true;
	}
	return false;
}


int User::deleteFile()
{
	int readnum;
	unsigned char sint[4];
	readnum = SSL_read(clntSock, (char *)sint, 4);
	while (readnum != 4)
	{
		if (readnum <= 0)
		{
			return false;
		}
		readnum += SSL_read(clntSock, (char *)sint + readnum, 4 - readnum);
	}
	int id = bytes_to_int(sint);

	readnum = SSL_read(clntSock, (char *)sint, 4);
	while (readnum != 4)
	{
		if (readnum <= 0)
		{
			return false;
		}
		readnum += SSL_read(clntSock, (char *)sint + readnum, 4 - readnum);
	}
	int deep = bytes_to_int(sint);
	if (Mysql->deleteFile(userid, id, deep))
	{
		char c[1];
		c[0] = SUCCESS_DELETE;
		SSL_write(clntSock, c, 1);
		return true;
	}
	else
	{
		char c[1];
		c[0] = FAILED_ACTION;
		SSL_write(clntSock, c, 1);
		return false;
	}
}

int User::upload_files(SSL * upSock)
{
	int readnum = 0;
	char c[4];
	//��ȡ�ļ�����С
	SSL_read(upSock, c, 1);
	int namelen = (unsigned char)c[0];
	char *filename = new char[namelen+1];
	//��ȡ�ļ���
	readnum = SSL_read(upSock, filename, namelen);
	while (readnum != namelen)
	{
		if (readnum <= 0)
		{
			delete[] filename;
			return -1;
		}
		 readnum += SSL_read(upSock, filename + readnum, namelen - readnum);
	}
	filename[namelen] = 0;
	//��ȡ�ļ���С
	readnum = SSL_read(upSock, c, 4);
	while (readnum != 4)
	{
		if (readnum <= 0)
		{
			delete[] filename;
			return -1;
		}
		readnum += SSL_read(upSock, c + readnum, 4 - readnum);
	}
	int filesize = bytes_to_int((unsigned char *)c);
	char fileID[33];
	//��ȡ�ļ��Ĺ�ϣ�Ĺ�ϣ
	readnum = SSL_read(upSock, fileID, 32);
	while (readnum != 32)
	{
		if (readnum <= 0)
		{
			delete[] filename;
			return -1;
		}
		readnum += SSL_read(upSock, fileID + readnum, 32 - readnum);
	}
	fileID[32] = 0;
	//��ȡ�������ܵ��ļ���Կ
	char fileKey[130];
	readnum = SSL_read(upSock, fileKey, 129);
	while (readnum != 129)
	{
		if (readnum <= 0)
		{
			delete[] filename;
			return -1;
		}
		readnum += SSL_read(upSock, fileKey + readnum, 129 - readnum);
	}
	unsigned char *locLen = new unsigned char[4];
	readnum = SSL_read(upSock, (char *)locLen, 4);
	while (readnum != 4)
	{
		if (readnum <= 0)
		{
			delete[] filename;
			delete[] locLen;
			return -1;
		}
		readnum += SSL_read(upSock, (char *)locLen + readnum, 4 - readnum);
	}
	//��ȡ�ļ�Ҫ�ŵ�λ��
	int deep = bytes_to_int(locLen);
	int loc;
	readnum = SSL_read(upSock, (char *)locLen, 4);
	while (readnum != 4)
	{
		if (readnum <= 0)
		{
			delete[] filename;
			return -1;
		}
		readnum += SSL_read(upSock, (char *)locLen + readnum, 4 - readnum);
	}
	loc = bytes_to_int(locLen);
	if (Mysql->fileExist(fileID))
	{
		srand((unsigned int)time(0));
		int left = ((((int)rand()) << 8) + rand()) % filesize;
		int temp = rand() % 32;
		int right = (left + temp) >= filesize ? filesize - 1 : left + temp;
		if (upload_partfile(filesize, fileID,upSock,left,right) && Mysql->writeFile(userid,filename, namelen, filesize, fileID, fileKey, loc, deep,1))
		{
			printf("�û�У���ļ��ɹ���\n");
			delete[] filename;
			delete[] locLen;
			return 1;
		}
		else
		{
			printf("�û��ϴ��ļ�ʧ�ܣ�\n");
			delete[] filename;
			delete[] locLen;
			return 0;
		}
	}
	else
	{
		char buffer[25];
		buffer[0] = SEND_FILE;
		randomiv(buffer + 1);
		int_to_bytes(1, (unsigned char *)(buffer + 17));
		int_to_bytes(filesize, (unsigned char *)(buffer + 21));
		SSL_write(upSock, buffer, 25);
		SSL_read(upSock, c, 1);
		if (c[0] == UPLOAD_FILE && recvfile(filesize, fileID, upSock) && Mysql->writeFile(userid, filename, namelen, filesize, fileID, fileKey, loc, deep, 1))
		{
			Mysql->writeFile(filesize, fileID);
			printf("�û��ϴ��ļ��ɹ���\n");
			delete[] filename;
			delete[] locLen;
			return 1;
		}
		else
		{
			printf("�û��ϴ��ļ�ʧ�ܣ�\n");
			delete[] filename;
			delete[] locLen;
			return 0;
		}
	}

}

bool User::upload_partfile(int filesize, char *fileID, SSL * upSock, int left,int right)
{

	//k���ֿ�����ļ���ָ������
	FILE **fd;

	//m���������ļ���ָ������
	FILE **fs;


	//��Ϊk����ļ������������ָ��
	char **data;

	//��Ϊm���������������ָ��
	char **coding;

	//��������ָ�룬���k��blocksize��С���ļ��������ݣ�Ҳ����data����
	char *buffer;

	char *checkdata;

	//�ļ����ֵĿ������
	int blocknum;

	//ȱʧ�Ŀ�ı�����飬���һλΪ-1
	int erasures[100];

	//��ʾȱʧ�Ŀ�����飬ĳ����Ϊ1˵�����±��Ӧ�Ŀ�ȱʧ
	int erase[100];

	int e = 0;
	//��ʱ�ļ���
	char fname[1000];

	int size = filesize * 16;
	//printf("��֤��ʼ��%ld\n", clock());
	memset(erase, 0, sizeof(char)* 100);
	memset(erasures, 0, sizeof(char)* 100);

	data = (char **)malloc(sizeof(char*)*ramp_k);
	coding = (char **)malloc(sizeof(char*)*ramp_m);

	fd = (FILE **)malloc(sizeof(FILE*)*ramp_k);
	fs = (FILE **)malloc(sizeof(FILE*)*ramp_m);

	for (int i = 0; i < ramp_m; i++)
	{
		coding[i] = (char *)malloc(sizeof(char)*blocksize);
		memset(coding[i], 0, sizeof(char)*ramp_w*packetsize);
	}
	//��֤�ļ��еĴ���
	char mname[256];
	
	for (int i = 0; i < ramp_k; i++)
	{
		sprintf_s(mname, 100, "file_k_%d", i);
		if (_access(mname, 0) == -1)
		{
			_mkdir(mname);
		}
	}
	for (int i = 0; i < ramp_m; i++)
	{
		sprintf_s(mname, 100, "file_m_%d", i);
		if (_access(mname, 0) == -1)
		{
			_mkdir(mname);
		}
	}
	char *temp;
	//�򿪸��ֿ�������������ļ�
	for (int i = 0; i < ramp_k; i++)
	{
		temp = charto16(fileID, 33);
		sprintf_s(fname, 100, "file_k_%d/%s", i, charto16(fileID, 33));
		free(temp);
		fopen_s(fd + i, fname, "rb");
		//���blocknum
		if (*(fd + i) != NULL)
		{
			fseek(*(fd + i), 0, SEEK_END);
			blocknum = ftell(*(fd + i)) / blocksize;
			fseek(*(fd + i), 0, SEEK_SET);
		}
		else{
			erasures[e++] = i;
			erase[i] = 1;
			temp = charto16(fileID, 33);
			sprintf_s(fname, 100, "file_k_%d/%s", i, charto16(fileID, 33));
			free(temp);
			fopen_s(fd + i, fname, "wb");
		}
	}
	for (int i = 0; i < ramp_m; i++)
	{
		temp = charto16(fileID, 33);
		sprintf_s(fname, 100, "file_m_%d/%s", i, charto16(fileID, 33));
		free(temp);
		fopen_s(fs + i, fname, "rb");
		if (*(fs + i) != NULL)
		{
			fseek(*(fs + i), 0, SEEK_END);
			blocknum = ftell(*(fs + i)) / blocksize;
			fseek(*(fs + i), 0, SEEK_SET);
		}
		else{
			erasures[e++] = i + ramp_k;
			erase[i + ramp_k] = 1;
			temp = charto16(fileID, 33);
			sprintf_s(fname, 100, "file_m_%d/%s", i, charto16(fileID, 33));
			free(temp);
			fopen_s(fs + i, fname, "wb");
		}
	}
	//��ý������ļ��Ĵ�С
	size = blocknum*blocksize*ramp_k;
	erasures[e] = -1;

	int xl = left * 16 / (blocksize*ramp_k);
	int xr;
	if (left * 16 % (blocksize*ramp_k) == 0)
		xl--;
	if (xl < 0)
		xl++;
	xr = xl + 1;

	if (e == ramp_m + ramp_k)
	{

		for (int i = 0; i < ramp_k; i++)
		{
			fflush(fd[i]);
			fclose(fd[i]);
		}
		free(fd);
		for (int i = 0; i < ramp_m; i++)
		{
			fflush(fs[i]);
			fclose(fs[i]);
		}
		free(fs);

		free(data);

		for (int i = 0; i < ramp_m; i++)
			free(coding[i]);
		free(coding);

		buffer = (char *)malloc(50 * sizeof(char));
		char *c = (char *)malloc(sizeof(char)* 4);
		memset(buffer, 0, sizeof(char)* 50);
		buffer[0] = SEND_FILE;
		randomiv(buffer + 1);
		int_to_bytes(1, (unsigned char *)(buffer + 17));
		int_to_bytes(filesize, (unsigned char *)(buffer + 21));
		SSL_write(upSock, buffer, 25);
		SSL_read(upSock, c, 1);
		if ((unsigned char)c[0] == UPLOAD_FILE&&recvfile(filesize, fileID, upSock) == 1)
		{
			free(c);
			free(buffer);
			return true;
		}
		else
		{
			free(c);
			free(buffer);
			return false;
		}
	}

	//������ڿ�ȱʧ�Ļ���Ҫ�Ȱ�ȱʧ�Ŀ鲹��
	if (erasures[0] != -1)
	{
		recover_file(fd, fs, erase, erasures, blocknum);
	}

	checkdata = (char *)malloc(sizeof(char)* 2 * blocksize*ramp_k);
	buffer = (char *)malloc(sizeof(char)* blocksize*ramp_k);
	int ct = xr - xl;
	for (int i = 0; i < ramp_k; i++)
	{
		//fseek�ƶ�����ȥ�����Բ��ùر��ļ��ٴ򿪵ķ�ʽ
		//fseek(fd[i], xl*blocksize, SEEK_SET);
		/*printf("%d\n", fseek(fd[i], xl*blocksize, SEEK_SET));*/
		fclose(fd[i]);
		temp = charto16(fileID, 33);
		sprintf_s(fname, 100, "file_k_%d/%s", i, temp);
		fopen_s(fd + i, fname, "rb");
		free(temp);
		fseek(fd[i], xl*blocksize, SEEK_SET);
	}
	memset(buffer, 0xff, sizeof(char)*blocksize*ramp_k);
	while (ct--)
	{
		memset(buffer, 0, sizeof(char)*blocksize*ramp_k);
		for (int i = 0; i < ramp_k; i++)
		{
			fread(buffer + i*blocksize, sizeof(char), blocksize, fd[i]);
			//printf("%s\n", charto16(buffer + i*blocksize));
		}
		int n = blocksize*ramp_k;
		memcpy_s(checkdata + ct*blocksize*ramp_k, blocksize*ramp_k, buffer, blocksize*ramp_k);
	}

	xl = ((left - 1) * 16 + blocksize*ramp_k) % (blocksize*ramp_k);
	char *sbuffer;
	//���������ļ�ĳ���ֵ��ļ�֡
	sbuffer = (char *)malloc(50 * sizeof(char));
	sbuffer[0] = SEND_FILE;
	memcpy_s(sbuffer + 1, 16, checkdata + xl, 16);
	int_to_bytes(left, (unsigned char *)(sbuffer + 17));
	int_to_bytes(right, (unsigned char *)(sbuffer + 21));
	//printf("%s\n", charto16(checkdata + xl));
	SSL_write(upSock, sbuffer, 25);
	char c[1];
	SSL_read(upSock, c, 1);
	bool ret;

	//֡��ʽ��ȷ������֤�ļ���ȷ����д������
	if ((unsigned char)c[0] == UPLOAD_FILE&&check_partfile(upSock, checkdata + xl, right - left + 1))
	{
		ret = true;
		//printf("��֤������%ld\n", clock());
	}
	else
	{
		ret = false;
	}
	free(buffer);
	free(sbuffer);

	free(checkdata);

	for (int i = 0; i < ramp_k; i++)
	{
		fflush(fd[i]);
		fclose(fd[i]);
	}
	free(fd);
	for (int i = 0; i < ramp_m; i++)
	{
		fflush(fs[i]);
		fclose(fs[i]);
	}
	free(fs);

	free(data);

	for (int i = 0; i < ramp_m; i++)
		free(coding[i]);
	free(coding);

	return ret;
}

//�ָ��ļ�
void User::recover_file(FILE **fd, FILE **fs, int *erase, int *erasures, int blocknum)
{
	//��Ϊk����ļ������������ָ��
	char **data;

	//��Ϊm���������������ָ��
	char **coding;

	//��������ָ�룬���k��blocksize��С���ļ��������ݣ�Ҳ����data����
	char *buffer;
	//printf("�ָ���ʼ��%ld\n", clock());

	data = (char **)malloc(sizeof(char*)*ramp_k);
	coding = (char **)malloc(sizeof(char*)*ramp_m);
	buffer = (char *)malloc(ramp_k*blocksize);

	for (int i = 0; i < ramp_m; i++)
	{
		coding[i] = (char *)malloc(sizeof(char)*blocksize);
		memset(coding[i], 0, sizeof(char)*ramp_w*packetsize);
	}


	while (blocknum--)
	{
		memset(buffer, 0, sizeof(char)*blocksize*ramp_k);
		//��ȡ���ݵ�data��coding
		for (int i = 0; i < ramp_k; i++)
		{
			if (erase[i] == 0)
				fread(buffer + i*blocksize, sizeof(char), blocksize, fd[i]);
			data[i] = buffer + i*blocksize;
		}
		for (int i = 0; i < ramp_m; i++)
		{
			if (erase[i + ramp_k] == 0)
				fread(coding[i], sizeof(char), blocksize, fs[i]);
		}

		jerasure_schedule_decode_lazy(ramp_k, ramp_m, ramp_w, bitmatrix, erasures, data, coding, packetsize*ramp_w, packetsize, 1);

		//���������ݿ�����鶪ʧ�Ļ����ָ���Щ��
		for (int i = 0; i < ramp_k; i++)
		{
			if (erase[i] != 0)
				fwrite(data[i], sizeof(char), blocksize, fd[i]);
		}
		for (int i = 0; i < ramp_m; i++)
		{
			if (erase[i + ramp_k] != 0)
				fwrite(coding[i], sizeof(char), blocksize, fs[i]);
		}
	}
	//printf("�ָ�������%ld\n", clock());
	for (int i = 0; i < ramp_k; i++)
	{
		fflush(fd[i]);
	}
	for (int i = 0; i < ramp_m; i++)
	{
		fflush(fs[i]);
	}

	free(data);

	for (int i = 0; i < ramp_m; i++)
		free(coding[i]);
	free(coding);

	free(buffer);
}

//���һ���ֵ��ļ��Ƿ���ͬ
bool User::check_partfile(SSL * upSock, char *checkdata, int ct)
{

	int readnum = 0;

	char *SSL_readdata;
	SSL_readdata = (char *)malloc(sizeof(char)*ct * 16);
	//���մ����Ĳ����ļ�
	readnum = SSL_read(upSock, SSL_readdata, ct * 16);
	while (readnum != ct * 16)
	{
		if (readnum <= 0)
		{
			free(SSL_readdata);
			return false;
		}
		readnum += SSL_read(upSock, SSL_readdata + readnum, ct * 16 - readnum);
	}
	/*printf("%s\n", charto16(checkdata));
	printf("-%s\n", charto16(SSL_readdata));*/
	//����ļ��Ƿ���ͬ
	for (int i = 0; i < ct * 16; i++)
	{
		if (checkdata[i] != SSL_readdata[i])
		{
			free(SSL_readdata);
			return false;
		}
	}
	free(SSL_readdata);
	return true;
}

int User::download_file(SSL * doSock)
{
	char fileID[33];

	memset(fileID, 0, 33 * sizeof(char));
	int readnum = 0;

	int sendnum = 0;
	readnum = SSL_read(doSock, fileID, 32);
	//��ȡ�ļ��Ĺ�ϣ�Ĺ�ϣ
	while (readnum != 32)
	{
		if (readnum <= 0)
		{
			return -1;
		}
		readnum += SSL_read(doSock, fileID + readnum, 32 - readnum);
	}
	fileID[32] = 0;
	//����û�ӵ�и��ļ����͸����û������ļ�
	int size = Mysql->fileIsPerson(fileID, userid);
	if (size>=0)
	{
		char *firstchar = (char *)malloc(sizeof(char));
		firstchar[0] = SEND_FILE;
		SSL_write(doSock, firstchar, 1);
		send_file(doSock, fileID, size);
		//free(filename);
		free(firstchar);
		return 1;
	}
	return 0;
}

bool User::share_file()
{
	int readnum = 0;
	char myfriend[33];
	char fileID[33];
	char fileKey[130];
	char c[4];
	bool ret = true;
	//��ȡ�������Ĺ�ϣ
	readnum = SSL_read(clntSock, myfriend, 32);
	while (readnum != 32)
	{
		if (readnum <= 0)
		{
			return false;
		}
		readnum += SSL_read(clntSock, myfriend + readnum, 32 - readnum);
	}
	myfriend[32] = 0;

	if (!Mysql->personIsFriend(userid, myfriend))
	{
		return false;
	}

	//��ȡ���ڵ���Ŀ
	readnum = SSL_read(clntSock, c, 4);
	while (readnum != 4)
	{
		if (readnum <= 0)
		{
			return false;
		}
		readnum += SSL_read(clntSock, c + readnum, 4 - readnum);
	}
	int rootnum = bytes_to_int((unsigned char *)c);

	char *filename=NULL;
	int filesize;
	int namelen;
	for (int i = 0; i < rootnum; i++)
	{
		//��ȡ�ļ�����С
		SSL_read(clntSock, c, 1);
		filename = (char *)malloc((unsigned char)c[0] * sizeof(char)+1);
		namelen = (unsigned char)c[0];
		//��ȡ�ļ���
		readnum = SSL_read(clntSock, filename, namelen);
		while (readnum != readnum)
		{
			if (readnum <= 0)
			{
				free(filename);
				return false;
			}
			readnum += SSL_read(clntSock, filename + readnum, namelen - readnum);
		}
		filename[namelen] = 0;
		//��ȡ�ļ���С
		readnum = SSL_read(clntSock, c, 4);
		while (readnum != 4)
		{
			if (readnum <= 0)
			{
				free(filename);
				return false;
			}
			readnum += SSL_read(clntSock, c + readnum, 4 - readnum);
		}
		filesize = bytes_to_int((unsigned char *)c);

		//��ȡ�ļ�����
		SSL_read(clntSock, c, 1);
		//�ļ��е����
		if (c[0] == 0){
			//��ȡ�ӽڵ���Ŀ
			int childnum;
			readnum = SSL_read(clntSock, c, 4);
			while (readnum != 4)
			{
				if (readnum <= 0)
				{
					free(filename);
					return false;
				}
				readnum += SSL_read(clntSock, c + readnum, 4 - readnum);
			}
			childnum = bytes_to_int((unsigned char *)c);
			char temp1[33];
			memset(temp1, 0xff, 32);
			temp1[32] = 0;
			char temp2[130];
			memset(temp2, 0xff, 129);
			temp2[129] = 0;
			int loc;
			if (Mysql->fileIsPerson(fileID, userid))
			{
				loc= Mysql->writeFile(myfriend, filename, namelen, 0, temp1, temp2, 0, 1, 0);
				ret &= writeTreeFiles(myfriend, loc, 2, childnum);
			}
			else
			{
				ret &= false;
			}
		}
		else
		{
			//�ļ������
			//��ȡ�ļ��Ĺ�ϣ�Ĺ�ϣ
			readnum = SSL_read(clntSock, fileID, 32);
			while (readnum != 32)
			{
				if (readnum <= 0)
				{
					free(filename);
					return false;
				}
				readnum += SSL_read(clntSock, fileID + readnum, 32 - readnum);
			}
			fileID[32] = 0;
			//��ȡ�������ܵ��ļ���Կ
			readnum = SSL_read(clntSock, fileKey, 129);
			while (readnum != 129)
			{
				if (readnum <= 0)
				{
					free(filename);
					return false;
				}
				readnum += SSL_read(clntSock, fileKey + readnum, 129 - readnum);
			}
			fileKey[129] = 0;
			//Ҫ���û����Է���Ļ���Ҫ��֤�ļ����û��У��ҷ�����Ǹ��û��ĺ���
			if (Mysql->fileIsPerson(fileID, userid))
			{
				Mysql->writeFile(myfriend, filename, namelen, filesize, fileID, fileKey, 0, 1, 1);
			}
			else
			{
				ret &= false;
			}
		}
	}
	if (ret)
	{
		c[0] = SUCCESS_SHARE;
		SSL_write(clntSock, c, 1);
		free(filename);
		return true;
	}
	else
	{
		c[0] = FAILED_ACTION;
		SSL_write(clntSock, c, 1);
		free(filename);
		return false;
	}
	
}

//�����д����״���ļ���Ϣ
bool User::writeTreeFiles(char *friendID,int loc, int deep, int num)
{
	char c[4];
	char *filename=NULL;
	int namelen;
	int readnum;
	int filesize;
	bool ret = true;
	char fileID[33];
	char fileKey[130];
	
	for (int i = 0; i < num; i++)
	{
		//��ȡ�ļ�����С
		SSL_read(clntSock, c, 1);
		filename = (char *)malloc((unsigned char)c[0] * sizeof(char)+1);
		namelen = (unsigned char)c[0];
		//��ȡ�ļ���
		readnum = SSL_read(clntSock, filename, namelen);
		while (readnum != readnum)
		{
			if (readnum <= 0)
			{
				free(filename);
				return false;
			}
			readnum += SSL_read(clntSock, filename + readnum, namelen - readnum);
		}
		filename[namelen] = 0;
		//��ȡ�ļ���С
		readnum = SSL_read(clntSock, c, 4);
		while (readnum != 4)
		{
			if (readnum <= 0)
			{
				free(filename);
				return false;
			}
			readnum += SSL_read(clntSock, c + readnum, 4 - readnum);
		}
		filesize = bytes_to_int((unsigned char *)c);

		//��ȡ�ļ�����
		SSL_read(clntSock, c, 1);
		//�ļ��е����
		if (c[0] == 0){
			//��ȡ�ӽڵ���Ŀ
			int childnum;
			readnum = SSL_read(clntSock, c, 4);
			while (readnum != 4)
			{
				if (readnum <= 0)
				{
					free(filename);
					return false;
				}
				readnum += SSL_read(clntSock, c + readnum, 4 - readnum);
			}
			childnum = bytes_to_int((unsigned char *)c);
			char temp1[33];
			memset(temp1, 0xff, 32);
			temp1[32] = 0;
			char temp2[130];
			memset(temp2, 0xff, 129);
			temp2[129] = 0;
			int newloc = 0;
			if (Mysql->fileIsPerson(fileID, userid))
			{
				int newloc = Mysql->writeFile(friendID, filename, namelen, 0, temp1, temp2, loc, deep, 0);
				ret &= writeTreeFiles(friendID,newloc, deep + 1, childnum);
			}
			else
			{
				ret &= false;
			}
		}
		else
		{
			//�ļ������
			//��ȡ�ļ��Ĺ�ϣ�Ĺ�ϣ
			readnum = SSL_read(clntSock, fileID, 32);
			while (readnum != 32)
			{
				if (readnum <= 0)
				{
					free(filename);
					return false;
				}
				readnum += SSL_read(clntSock, fileID + readnum, 32 - readnum);
			}
			fileID[32] = 0;
			//��ȡ�������ܵ��ļ���Կ
			readnum = SSL_read(clntSock, fileKey, 129);
			while (readnum != 129)
			{
				if (readnum <= 0)
				{
					free(filename);
					return false;
				}
				readnum += SSL_read(clntSock, fileKey + readnum, 129 - readnum);
			}
			fileKey[129] = 0;
			//Ҫ���û����Է���Ļ���Ҫ��֤�ļ����û��У��ҷ�����Ǹ��û��ĺ���
			if (Mysql->fileIsPerson(fileID, userid))
			{
				Mysql->writeFile(friendID, filename, namelen, filesize, fileID, fileKey, loc, deep, 1);
			}
			else
			{
				ret &= false;
			}
		}
	}
	if (ret)
	{
		free(filename);
		return true;
	}
	else
	{
		free(filename);
		return false;
	}

}


void User::send_file(SSL * clntSock, char *filename, int size)
{

	//k���ֿ�����ļ���ָ������
	FILE **fd;

	//m���������ļ���ָ������
	FILE **fs;


	//��Ϊk����ļ������������ָ��
	char **data;

	//��Ϊm���������������ָ��
	char **coding;

	//��������ָ�룬���k��blocksize��С���ļ��������ݣ�Ҳ����data����
	char *buffer;

	//�ļ����ֵĿ������
	int blocknum;

	//ȱʧ�Ŀ�ı�����飬���һλΪ-1
	int erasures[100];

	//��ʾȱʧ�Ŀ�����飬ĳ����Ϊ1˵�����±��Ӧ�Ŀ�ȱʧ
	int erase[100];

	int e = 0;
	//��ʱ�ļ���
	char fname[1000];

	size = size * 16;

	memset(erase, 0, sizeof(erase));

	data = (char **)malloc(sizeof(char*)*ramp_k);
	coding = (char **)malloc(sizeof(char*)*ramp_m);
	buffer = (char *)malloc(ramp_k*blocksize);
	fd = (FILE **)malloc(sizeof(FILE*)*ramp_k);
	fs = (FILE **)malloc(sizeof(FILE*)*ramp_m);

	for (int i = 0; i < ramp_m; i++)
	{
		coding[i] = (char *)malloc(sizeof(char)*blocksize);
		memset(coding[i], 0, sizeof(char)*ramp_w*packetsize);
	}

	//��֤�ļ��еĴ���
	char mname[256];
	if (_access("user_file", 0) == -1)
	{
		_mkdir("user_file");
	}
	for (int i = 0; i < ramp_k; i++)
	{
		sprintf_s(mname, 100, "file_k_%d", i);
		if (_access(mname, 0) == -1)
		{
			_mkdir(mname);
		}
	}
	for (int i = 0; i < ramp_m; i++)
	{
		sprintf_s(mname, 100, "file_m_%d", i);
		if (_access(mname, 0) == -1)
		{
			_mkdir(mname);
		}
	}

	char *temp;
	//�򿪸��ֿ�������������ļ�
	for (int i = 0; i < ramp_k; i++)
	{
		temp = charto16(filename, 33);
		sprintf_s(fname, 100, "file_k_%d/%s", i, temp);
		fopen_s(fd + i, fname, "rb");
		free(temp);
		//���blocknum
		if (*(fd + i) != NULL)
		{
			fseek(*(fd + i), 0, SEEK_END);
			blocknum = ftell(*(fd + i)) / blocksize;
			fseek(*(fd + i), 0, SEEK_SET);
		}
		else{
			erasures[e++] = i;
			erase[i] = 1;
			temp = charto16(filename, 33);
			sprintf_s(fname, 100, "file_k_%d/%s", i, charto16(filename, 33));
			fopen_s(fd + i, fname, "wb");
			free(temp);
		}
	}
	for (int i = 0; i < ramp_m; i++)
	{
		temp = charto16(filename, 33);
		sprintf_s(fname, 100, "file_m_%d/%s", i, charto16(filename, 33));
		free(temp);
		fopen_s(fs + i, fname, "rb");
		if (*(fs + i) != NULL)
		{
			fseek(*(fs + i), 0, SEEK_END);
			blocknum = ftell(*(fs + i)) / blocksize;
			fseek(*(fs + i), 0, SEEK_SET);
		}
		else{
			erasures[e++] = i + ramp_k;
			erase[i + ramp_k] = 1;
			temp = charto16(filename, 33);
			sprintf_s(fname, 100, "file_m_%d/%s", i, charto16(filename, 33));
			fopen_s(fs + i, fname, "wb");
			free(temp);
		}
	}


	//��ý������ļ��Ĵ�С
	size = blocknum*blocksize*ramp_k;
	erasures[e] = -1;

	//printf("���ؿ�ʼ��%ld\n", clock());

	while (blocknum--)
	{
		memset(buffer, 0, sizeof(char)*blocksize*ramp_k);
		//��ȡ���ݵ�data��coding
		for (int i = 0; i < ramp_k; i++)
		{
			if (erase[i] == 0)
				fread(buffer + i*blocksize, sizeof(char), blocksize, fd[i]);
			data[i] = buffer + i*blocksize;
		}
		for (int i = 0; i < ramp_m; i++)
		{
			if (erase[i + ramp_k] == 0)
				fread(coding[i], sizeof(char), blocksize, fs[i]);
		}

		//���û�п춪ʧ���Ͳ��ý���
		if (erasures[0] != -1)
			jerasure_schedule_decode_lazy(ramp_k, ramp_m, ramp_w, bitmatrix, erasures, data, coding, packetsize*ramp_w, packetsize, 1);

		//һ�����ļ���д����ֽ���
		int n = blocksize*ramp_k;
		//��ȥ���
		if (blocknum == 0)
		{
			while (buffer[--n] == 0);
			
		}
		SSL_write(clntSock, buffer, n);

		//���������ݿ�����鶪ʧ�Ļ�����Ҫ�ָ���Щ��
		for (int i = 0; i < ramp_k; i++)
		{
			if (erase[i] != 0)
				fwrite(data[i], sizeof(char), blocksize, fd[i]);
		}
		for (int i = 0; i < ramp_m; i++)
		{
			if (erase[i + ramp_k] != 0)
				fwrite(coding[i], sizeof(char), blocksize, fs[i]);
		}
	}
	//printf("���ؽ�����%ld\n", clock());

	//�ͷ���������Ŀռ�
	for (int i = 0; i < ramp_k; i++)
	{
		fflush(fd[i]);
		fclose(fd[i]);
	}
	free(fd);
	for (int i = 0; i < ramp_m; i++)
	{
		fflush(fs[i]);
		fclose(fs[i]);
	}
	free(fs);


	free(data);

	for (int i = 0; i < ramp_m; i++)
		free(coding[i]);
	free(coding);

	free(buffer);

	return;
}

bool User::recvfile(int size,char *filename,SSL * upSock)
{
	//k���ֿ�����ļ���ָ������
	FILE **fd;

	//m���������ļ���ָ������
	FILE **fs;


	//��Ϊk����ļ������������ָ��
	char **data;

	//��Ϊm���������������ָ��
	char **coding;

	//��������ָ�룬���k��blocksize��С���ļ��������ݣ�Ҳ����data����
	char *buffer;

	//�ļ����ֵĿ������
	int blocknum;


	//��ʱ�ļ���
	char fname[1000];
	//cout << size << endl;

	size = size * 16;

	//printf("�ϴ���ʼ��%ld\n", clock());

	data = (char **)malloc(sizeof(char*)*ramp_k);
	coding = (char **)malloc(sizeof(char*)*ramp_m);
	buffer = (char *)malloc(ramp_k*blocksize);
	memset(buffer, 0, ramp_k*blocksize*sizeof(char));
	fd = (FILE **)malloc(sizeof(FILE*)*ramp_k);
	fs = (FILE **)malloc(sizeof(FILE*)*ramp_m);
	for (int i = 0; i < ramp_m; i++)
	{
		coding[i] = (char *)malloc(sizeof(char)*blocksize);
		memset(coding[i], 0, sizeof(char)*ramp_w*packetsize);
	}

	//��֤�ļ��еĴ���
	char mname[256];
	for (int i = 0; i < ramp_k; i++)
	{
		sprintf_s(mname, 100, "file_k_%d", i);
		if (_access(mname, 0) == -1)
		{
			_mkdir(mname);
		}
	}
	for (int i = 0; i < ramp_m; i++)
	{
		sprintf_s(mname, 100, "file_m_%d", i);
		if (_access(mname, 0) == -1)
		{
			_mkdir(mname);
		}
	}

	//�򿪷ֿ���ļ��ͱ�����ļ�
	char *temp;
	for (int i = 0; i < ramp_k; i++)
	{
		temp = charto16(filename, 33);
		sprintf_s(fname, 100, "file_k_%d/%s", i, temp);
		fopen_s(fd + i, fname, "wb");
		free(temp);
	}
	for (int i = 0; i < ramp_m; i++)
	{
		temp = charto16(filename, 33);
		sprintf_s(fname, 100, "file_m_%d/%s", i, charto16(filename,33));
		fopen_s(fs + i, fname, "wb");
		free(temp);
	}
	if (fd[0] == NULL)
	{
		free(fd);
		free(fs);
		free(data);

		for (int i = 0; i < ramp_m; i++)
			free(coding[i]);
		free(coding);

		free(buffer);

		return false;
	}
	blocknum = size / (blocksize*ramp_k) + 1;



	//��ȡ�ļ����ֿ����౸��
	while (blocknum--)
	{
		int haveread = 0;
		if (blocknum != 0)
		{
			haveread = SSL_read(upSock, buffer, blocksize*ramp_k);
			while (haveread != blocksize*ramp_k)
			{
				if (haveread <= 0)
				{
					//�ͷ���������Ŀռ�
					for (int i = 0; i < ramp_k; i++)
					{
						fflush(fd[i]);
						fclose(fd[i]);
					}
					free(fd);
					for (int i = 0; i < ramp_m; i++)
					{
						fflush(fs[i]);
						fclose(fs[i]);
					}
					free(fs);
					free(data);

					for (int i = 0; i < ramp_m; i++)
						free(coding[i]);
					free(coding);

					free(buffer);

					return false;
				}
				haveread += SSL_read(upSock, buffer + haveread, blocksize*ramp_k - haveread);
			}
		}
		else
		{
			haveread = SSL_read(upSock, buffer, size % (blocksize*ramp_k));
			while (haveread != size % (blocksize*ramp_k))
			{
				//printf("%d %d\n", blocknum, haveread);
				if (haveread <= 0)
				{
					//�ͷ���������Ŀռ�
					for (int i = 0; i < ramp_k; i++)
					{
						fflush(fd[i]);
						fclose(fd[i]);
					}
					free(fd);
					for (int i = 0; i < ramp_m; i++)
					{
						fflush(fs[i]);
						fclose(fs[i]);
					}
					free(fs);
					free(data);

					for (int i = 0; i < ramp_m; i++)
						free(coding[i]);
					free(coding);

					free(buffer);

					return false;
				}
				haveread += SSL_read(upSock, buffer + haveread, size % (blocksize*ramp_k) - haveread);
			}
			//����memsetһ��ÿһ��ĺ������1000������
			buffer[size % (blocksize*ramp_k)] = 1;
		}

		//���ݼ�Ϊbuffer�е�����
		for (int i = 0; i < ramp_k; i++)
		{
			data[i] = buffer + i*blocksize;
		}


		//�����ļ�
		jerasure_schedule_encode(ramp_k, ramp_m, ramp_w, schedule, data, coding, blocksize, packetsize);

		//д���ļ�
		for (int i = 0; i < ramp_k; i++)
		{
			fwrite(data[i], sizeof(char), blocksize, fd[i]);
		}
		for (int i = 0; i < ramp_m; i++)
		{
			fwrite(coding[i], sizeof(char), blocksize, fs[i]);
		}
		//��ջ�����
		memset(buffer, 0, sizeof(char)*blocksize*ramp_k);
	}
	//printf("�ϴ�������%ld\n", clock());

	//�ͷ���������Ŀռ�
	for (int i = 0; i < ramp_k; i++)
	{
		fflush(fd[i]);
		fclose(fd[i]);
	}
	free(fd);
	for (int i = 0; i < ramp_m; i++)
	{
		fflush(fs[i]);
		fclose(fs[i]);
	}
	free(fs);


	free(data);

	for (int i = 0; i < ramp_m; i++)
		free(coding[i]);
	free(coding);

	free(buffer);

	return true;
}

//�û��ĵ�¼
User *sign_in(SSL * clntSock)
{
	char userid[33];
	char userpassword[33];
	char cookies[33];
	int readnum = 0;
	//��ȡ�û����Ĺ�ϣ
	readnum = SSL_read(clntSock, userid, 32);
	while (readnum != 32)
	{
		if (readnum <= 0)
			return NULL;
		readnum += SSL_read(clntSock, userid + readnum, 32 - readnum);
	}
	userid[32] = 0;

	//��ȡ����
	readnum = SSL_read(clntSock, userpassword, 32);
	while (readnum != 32)
	{
		if (readnum <= 0)
			return NULL;
		readnum += SSL_read(clntSock, userpassword + readnum, 32 - readnum);
	}
	userpassword[32] = 0;
	//�жϸ��û�������������ȷ�Ҹ��û�û�е�¼
	if (Mysql->personLogin(userid,userpassword))
	{
		char f[1];
		f[0] = SUCCESS_LOGN;
		randomiv(cookies);
		randomiv(cookies + 16);
		SSL_write(clntSock, f, 1);
		SSL_write(clntSock, cookies, 32);
		char *hexcookies=charto16(cookies,33);
		hexcookies[65] = 0;
		User *user = new User(userid, userpassword, clntSock);
		all_users[hexcookies] = user;
		free(hexcookies);
		return user;
	}
	else
	{
		char f[1];
		f[0] = FAILED_ACTION;
		SSL_write(clntSock, f, 1);
		return NULL;
	}
}

//�û�ע��
User *sign_up(SSL * clntSock)
{
	char userid[33];
	char userpassword[33];
	char cookies[33];
	int readnum = 0;
	//��ȡ�û����Ĺ�ϣ
	readnum = SSL_read(clntSock, userid, 32);
	while (readnum != 32)
	{
		if (readnum <= 0)
			return NULL;
		readnum += SSL_read(clntSock, userid + readnum, 32 - readnum);
	}
	userid[32] = 0;
	//��ȡ����
	readnum = SSL_read(clntSock, userpassword, 32);
	while (readnum != 32)
	{
		if (readnum <= 0)
			return NULL;
		readnum += SSL_read(clntSock, userpassword + readnum, 32 - readnum);
	}
	userpassword[32] = 0;

	char c[1];
	SSL_read(clntSock,c, 1);
	int namelen = (unsigned char)c[0];
	char *name = (char *)malloc((namelen + 1)*sizeof(char));
	readnum = SSL_read(clntSock, name,namelen);
	while (readnum != namelen)
	{
		if (readnum <= 0)
		{
			return NULL;
		}
		readnum += SSL_read(clntSock, name + readnum, namelen - readnum);
	}
	name[namelen] = 0;
	char userPkey[66];
	readnum = SSL_read(clntSock, userPkey, 65);
	while (readnum != 65)
	{
		if (readnum <= 0)
		{
			return NULL;
		}
		readnum += SSL_read(clntSock, userPkey + readnum, 65 - readnum);
	}
	userPkey[65] = 0;
	//��������û������ڵĻ�������ע��
	if (!Mysql->personExist(userid))
	{
		Mysql->personInsert(userid, userpassword, userPkey, name, namelen);
		char f[1];
		f[0] = SUCCESS_LOGN;
		randomiv(cookies);
		randomiv(cookies + 16);
		SSL_write(clntSock, f, 1);
		SSL_write(clntSock, cookies, 32);
		char *hexcookies = charto16(cookies, 33);
		hexcookies[65] = 0;
		User *user = new User(userid, userpassword, clntSock);
		all_users[hexcookies] = user;
		free(hexcookies);
		free(name);
		return user;
	}
	else
	{
		char f[1];
		f[0] = FAILED_ACTION;
		SSL_write(clntSock, f, 1);
		free(name);
		return NULL;
	}
}

int bytes_to_int(unsigned char *b)
{
	int i = 0;
	for (int j = 3; j >= 0; j--)
	{
		i = i << 8;
		i += b[j];
	}
	return i;
}

void int_to_bytes(int i, unsigned char *b)
{
	memset(b, 0, sizeof(unsigned char)* 4);
	for (int j = 0; j < 4; j++)
	{
		b[j] = i % 256;
		i = i / 256;
	}
	return;
}

void randomiv(char *iv)
{
	srand((unsigned int)time(0));
	for (int i = 0; i < 16; i++)
		iv[i] = (char)(rand() % 256);
	return;
}


char *charto16(char *s,int size)
{
	char *c = (char *)malloc(size*2 * sizeof(char));
	memset(c, 0, size * 2 * sizeof(char));
	for (int i = 0; i < size-1; i++)
		sprintf_s(c + 2 * i, size * 2 - 2 * i, "%02x ", (unsigned char)s[i]);
	//c[size*2-2] = 0;
	return c;
}

//sizeָ��ת��֮��Ĵ�С
void hextochar(char *src, char *des, int size)
{
	memset(des, 0, size);
	for (int i = 0; i < size; i++)
	{
		if (src[2*i] <= '9'&&src[2*i] >= '0')
			des[i] += src[2*i] - '0';
		else
			des[i] += src[2*i] - 'a' + 10;
		des[i] *= 16;
		if (src[2*i+1] <= '9'&&src[2*i+1] >= '0')
			des[i] += src[2*i+1] - '0';
		else
			des[i] += src[2*i+1] - 'a' + 10;
	}
}