#include"user.h"
#include"MySQLManager.h"

MySQLManager *Mysql;

//文件分为k份
int ramp_k;

//产生m块冗余码
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

	//收到的字节数
	int readnum = 0;
	int flag = 1;


	while (flag)
	{
		//printf("子线程%d\n", GetCurrentThreadId());
		//printf("等待");
		readnum = SSL_read(clntSock, buffer, 1);
		if (readnum <= 0)
		{
			printf("用户已断开连接!\n");
			sign_out();
			flag = 0;
			return;
		}
		else
		{
			switch (buffer[0]){
			case SIGN_OUT:
				//这个判断的目的主要是判断数据库有没有出错
				if (sign_out())
				{
					printf("一用户已退出...\n");
					//一旦退出就返回
					return;
				}
				break;
			case REQUIRE_FILES:
				//除非数据库出现错误，否则请求文件列表一定成功
				require_files();
				printf("一用户已取得文件列表！\n");
				break;
			case REQUIRE_FRIENDS:
				require_friends();			
				printf("一用户取得好友列表成功！\n");
				break;
			case ADD_FRIEND:
				if(add_friend())
					printf("一用户已添加一好友！\n");
				else
					printf("一用户添加好友失败！\n");
				break;
			case DELETE_FILE:
				if (deleteFile())
					printf("一用户删除文件成功！\n");
				else
					printf("一用户删除文件失败！\n");
				break;
			case CREATE_DIR:
				if (createDir())
					printf("一用户已创建目录\n");
				else
					printf("一用户创建目录失败\n");
				break;
			case SHARE_FILE:
				if (share_file())
				{
					printf("一用户分享文件成功！\n");
				}
				else
				{
					printf("一用户分享文件失败！\n");
				}
				break;
			default:
				flag = 0;
				break;
			}
		}
	}
	printf("用户不合法的行为导致连接被主动关闭！\n");
}



int User::sign_out()
{
	return Mysql->personLogout(userid);
}


//返回数据帧格式：标志（1）|文件个数（4）|id（4）|文件id（32）|文件名长度（1）|文件名（*）|文件大小（4）|文件公钥（129）|文件类型（1）|*
//用户请求文件列表
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

//它应该是没有返回值的
int User::require_friends()
{
	char *buffer=NULL;
	int len = Mysql->getFriends(userid, &buffer);
	
	SSL_write(clntSock, buffer, len);
	delete[] buffer;
	return 0;
}

//用户添加好友，只需要发送该好友的名字的哈希就可以了，会返回给用户好友的公钥
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
	//获取文件名大小
	SSL_read(upSock, c, 1);
	int namelen = (unsigned char)c[0];
	char *filename = new char[namelen+1];
	//获取文件名
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
	//获取文件大小
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
	//获取文件的哈希的哈希
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
	//获取经过加密的文件密钥
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
	//获取文件要放的位置
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
			printf("用户校验文件成功！\n");
			delete[] filename;
			delete[] locLen;
			return 1;
		}
		else
		{
			printf("用户上传文件失败！\n");
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
			printf("用户上传文件成功！\n");
			delete[] filename;
			delete[] locLen;
			return 1;
		}
		else
		{
			printf("用户上传文件失败！\n");
			delete[] filename;
			delete[] locLen;
			return 0;
		}
	}

}

bool User::upload_partfile(int filesize, char *fileID, SSL * upSock, int left,int right)
{

	//k个分开后的文件的指针数组
	FILE **fd;

	//m个冗余块的文件的指针数组
	FILE **fs;


	//分为k块的文件的数据数组的指针
	char **data;

	//分为m块的冗余码的数组的指针
	char **coding;

	//缓冲区的指针，存放k块blocksize大小的文件数据内容，也就是data数据
	char *buffer;

	char *checkdata;

	//文件划分的块的总数
	int blocknum;

	//缺失的块的编号数组，最后一位为-1
	int erasures[100];

	//标示缺失的块的数组，某个数为1说明其下标对应的块缺失
	int erase[100];

	int e = 0;
	//临时文件名
	char fname[1000];

	int size = filesize * 16;
	//printf("验证开始：%ld\n", clock());
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
	//保证文件夹的存在
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
	//打开各分块和冗余码所在文件
	for (int i = 0; i < ramp_k; i++)
	{
		temp = charto16(fileID, 33);
		sprintf_s(fname, 100, "file_k_%d/%s", i, charto16(fileID, 33));
		free(temp);
		fopen_s(fd + i, fname, "rb");
		//获得blocknum
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
	//获得解码后的文件的大小
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

	//假如存在块缺失的话，要先把缺失的块补齐
	if (erasures[0] != -1)
	{
		recover_file(fd, fs, erase, erasures, blocknum);
	}

	checkdata = (char *)malloc(sizeof(char)* 2 * blocksize*ramp_k);
	buffer = (char *)malloc(sizeof(char)* blocksize*ramp_k);
	int ct = xr - xl;
	for (int i = 0; i < ramp_k; i++)
	{
		//fseek移动不过去，所以采用关闭文件再打开的方式
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
	//发送请求文件某部分的文件帧
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

	//帧格式正确并且验证文件正确，就写入数据
	if ((unsigned char)c[0] == UPLOAD_FILE&&check_partfile(upSock, checkdata + xl, right - left + 1))
	{
		ret = true;
		//printf("验证结束：%ld\n", clock());
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

//恢复文件
void User::recover_file(FILE **fd, FILE **fs, int *erase, int *erasures, int blocknum)
{
	//分为k块的文件的数据数组的指针
	char **data;

	//分为m块的冗余码的数组的指针
	char **coding;

	//缓冲区的指针，存放k块blocksize大小的文件数据内容，也就是data数据
	char *buffer;
	//printf("恢复开始：%ld\n", clock());

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
		//读取数据到data和coding
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

		//假如有数据块或编码块丢失的话，恢复这些块
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
	//printf("恢复结束：%ld\n", clock());
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

//检查一部分的文件是否相同
bool User::check_partfile(SSL * upSock, char *checkdata, int ct)
{

	int readnum = 0;

	char *SSL_readdata;
	SSL_readdata = (char *)malloc(sizeof(char)*ct * 16);
	//接收传来的部分文件
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
	//检查文件是否相同
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
	//读取文件的哈希的哈希
	while (readnum != 32)
	{
		if (readnum <= 0)
		{
			return -1;
		}
		readnum += SSL_read(doSock, fileID + readnum, 32 - readnum);
	}
	fileID[32] = 0;
	//如果用户拥有该文件，就给该用户传该文件
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
	//获取好友名的哈希
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

	//获取根节点数目
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
		//获取文件名大小
		SSL_read(clntSock, c, 1);
		filename = (char *)malloc((unsigned char)c[0] * sizeof(char)+1);
		namelen = (unsigned char)c[0];
		//获取文件名
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
		//获取文件大小
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

		//获取文件类型
		SSL_read(clntSock, c, 1);
		//文件夹的情况
		if (c[0] == 0){
			//获取子节点数目
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
			//文件的情况
			//获取文件的哈希的哈希
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
			//获取经过加密的文件密钥
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
			//要是用户可以分享的话，要保证文件该用户有，且分享的是该用户的好友
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

//向好友写入树状的文件信息
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
		//获取文件名大小
		SSL_read(clntSock, c, 1);
		filename = (char *)malloc((unsigned char)c[0] * sizeof(char)+1);
		namelen = (unsigned char)c[0];
		//获取文件名
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
		//获取文件大小
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

		//获取文件类型
		SSL_read(clntSock, c, 1);
		//文件夹的情况
		if (c[0] == 0){
			//获取子节点数目
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
			//文件的情况
			//获取文件的哈希的哈希
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
			//获取经过加密的文件密钥
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
			//要是用户可以分享的话，要保证文件该用户有，且分享的是该用户的好友
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

	//k个分开后的文件的指针数组
	FILE **fd;

	//m个冗余块的文件的指针数组
	FILE **fs;


	//分为k块的文件的数据数组的指针
	char **data;

	//分为m块的冗余码的数组的指针
	char **coding;

	//缓冲区的指针，存放k块blocksize大小的文件数据内容，也就是data数据
	char *buffer;

	//文件划分的块的总数
	int blocknum;

	//缺失的块的编号数组，最后一位为-1
	int erasures[100];

	//标示缺失的块的数组，某个数为1说明其下标对应的块缺失
	int erase[100];

	int e = 0;
	//临时文件名
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

	//保证文件夹的存在
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
	//打开各分块和冗余码所在文件
	for (int i = 0; i < ramp_k; i++)
	{
		temp = charto16(filename, 33);
		sprintf_s(fname, 100, "file_k_%d/%s", i, temp);
		fopen_s(fd + i, fname, "rb");
		free(temp);
		//获得blocknum
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


	//获得解码后的文件的大小
	size = blocknum*blocksize*ramp_k;
	erasures[e] = -1;

	//printf("下载开始：%ld\n", clock());

	while (blocknum--)
	{
		memset(buffer, 0, sizeof(char)*blocksize*ramp_k);
		//读取数据到data和coding
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

		//如果没有快丢失，就不用解码
		if (erasures[0] != -1)
			jerasure_schedule_decode_lazy(ramp_k, ramp_m, ramp_w, bitmatrix, erasures, data, coding, packetsize*ramp_w, packetsize, 1);

		//一次向文件中写入的字节数
		int n = blocksize*ramp_k;
		//除去填充
		if (blocknum == 0)
		{
			while (buffer[--n] == 0);
			
		}
		SSL_write(clntSock, buffer, n);

		//假如有数据块或编码块丢失的话，仍要恢复这些块
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
	//printf("下载结束：%ld\n", clock());

	//释放所有申请的空间
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
	//k个分开后的文件的指针数组
	FILE **fd;

	//m个冗余块的文件的指针数组
	FILE **fs;


	//分为k块的文件的数据数组的指针
	char **data;

	//分为m块的冗余码的数组的指针
	char **coding;

	//缓冲区的指针，存放k块blocksize大小的文件数据内容，也就是data数据
	char *buffer;

	//文件划分的块的总数
	int blocknum;


	//临时文件名
	char fname[1000];
	//cout << size << endl;

	size = size * 16;

	//printf("上传开始：%ld\n", clock());

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

	//保证文件夹的存在
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

	//打开分块的文件和编码的文件
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



	//读取文件，分块冗余备份
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
					//释放所有申请的空间
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
					//释放所有申请的空间
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
			//这句和memset一起将每一块的后面填充1000···
			buffer[size % (blocksize*ramp_k)] = 1;
		}

		//数据即为buffer中的数据
		for (int i = 0; i < ramp_k; i++)
		{
			data[i] = buffer + i*blocksize;
		}


		//编码文件
		jerasure_schedule_encode(ramp_k, ramp_m, ramp_w, schedule, data, coding, blocksize, packetsize);

		//写入文件
		for (int i = 0; i < ramp_k; i++)
		{
			fwrite(data[i], sizeof(char), blocksize, fd[i]);
		}
		for (int i = 0; i < ramp_m; i++)
		{
			fwrite(coding[i], sizeof(char), blocksize, fs[i]);
		}
		//清空缓冲区
		memset(buffer, 0, sizeof(char)*blocksize*ramp_k);
	}
	//printf("上传结束：%ld\n", clock());

	//释放所有申请的空间
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

//用户的登录
User *sign_in(SSL * clntSock)
{
	char userid[33];
	char userpassword[33];
	char cookies[33];
	int readnum = 0;
	//读取用户名的哈希
	readnum = SSL_read(clntSock, userid, 32);
	while (readnum != 32)
	{
		if (readnum <= 0)
			return NULL;
		readnum += SSL_read(clntSock, userid + readnum, 32 - readnum);
	}
	userid[32] = 0;

	//读取密码
	readnum = SSL_read(clntSock, userpassword, 32);
	while (readnum != 32)
	{
		if (readnum <= 0)
			return NULL;
		readnum += SSL_read(clntSock, userpassword + readnum, 32 - readnum);
	}
	userpassword[32] = 0;
	//判断该用户存在且密码正确且该用户没有登录
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

//用户注册
User *sign_up(SSL * clntSock)
{
	char userid[33];
	char userpassword[33];
	char cookies[33];
	int readnum = 0;
	//读取用户名的哈希
	readnum = SSL_read(clntSock, userid, 32);
	while (readnum != 32)
	{
		if (readnum <= 0)
			return NULL;
		readnum += SSL_read(clntSock, userid + readnum, 32 - readnum);
	}
	userid[32] = 0;
	//读取密码
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
	//假如这个用户不存在的话，可以注册
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

//size指的转了之后的大小
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