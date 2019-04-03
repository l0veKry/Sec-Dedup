#include"user.h"
#include"MySQLManager.h"


#pragma comment(lib, "ws2_32.lib")
#pragma comment(lib,"C:\\Program Files\\MySQL\\MySQL Server 5.7\\lib\\libmysql.lib")
#pragma comment( lib, "libeay32.lib" )  
#pragma comment( lib, "ssleay32.lib" )  


extern map<string, User *> all_users;
//�ļ���Ϊk��
extern int ramp_k;

//����m��������
extern int ramp_m;

//word size
extern int ramp_w;


extern MySQLManager *Mysql;
//
extern int blocksize;
extern int packetsize;
//extern HANDLE filelock;
//extern HANDLE userlock;
//
extern int *matrix;
extern int *bitmatrix;
extern int **schedule;
//
////ϵͳ��ʼ��
void initialize();
//
int cnt;
SSL_CTX *ctx;

//�̺߳���
unsigned int __stdcall  thread_run(void *sock);

int main()
{
	FILE *ini=NULL;
	fopen_s(&ini, "set1.ini", "r");
	if (ini == NULL)
	{
		printf("û���ҵ�Ĭ�������ļ���");
		system("pause");
		return 0;
	}
	char address[20] = "127.0.0.1";
	int port = 1234;
	ramp_k = 2;
	ramp_m = 2;
	char sqladdr[100];
	char name[100];
	char password[100];
	//printf("����ip��ַ��");
	fscanf_s(ini,"[ip]%s", address, 20);
	//printf("����˿ںţ�");
	fscanf_s(ini, " [port]%d", &port);
	//printf("�������ݷֿ���k���������m��");
	fscanf_s(ini, " [k]%d[m]%d", &ramp_k, &ramp_m);

	//printf("�������ݿ�ip��ַ,�û���������:");
	fscanf_s(ini," [mysql-ip] %s", sqladdr, 100);
	fscanf_s(ini," [mysql-u] %s", name, 100);
	fscanf_s(ini," [mysql-p] %s", password, 100);


	Mysql = new MySQLManager(sqladdr, name, password, "mysql");

	printf("ϵͳ��ʼ���С�����\n");

	ramp_w = 8;
	packetsize = 1024;
	blocksize = packetsize*ramp_w;

	matrix = cauchy_good_general_coding_matrix(ramp_k, ramp_m, ramp_w);
	bitmatrix = jerasure_matrix_to_bitmatrix(ramp_k, ramp_m, ramp_w, matrix);
	schedule = jerasure_smart_bitmatrix_to_schedule(ramp_k, ramp_m, ramp_w, bitmatrix);



	WSADATA wsaData;
	WSAStartup(MAKEWORD(2, 2), &wsaData);
	SOCKET servSock = socket(PF_INET, SOCK_STREAM, IPPROTO_TCP);
	if (servSock < 0)
	{
		printf("socket failed!");
		system("pause");
		exit(0);
	}

	SOCKADDR_IN sockAddr;
	memset(&sockAddr, 0, sizeof(sockAddr));  //ÿ���ֽڶ���0���
	sockAddr.sin_family = PF_INET;  //ʹ��IPv4��ַ
	sockAddr.sin_addr.s_addr = htonl(INADDR_ANY);;  //�����IP��ַ
	sockAddr.sin_port = htons(port);  //�˿�
	if (bind(servSock, (SOCKADDR*)&sockAddr, sizeof(SOCKADDR)) < 0)
	{
		printf("bind error!\n");
		system("pause");
		exit(1);
	}
	//�������״̬
	listen(servSock, 10000);


	//ssl����
	SSL_library_init();/* SSL ���ʼ�� */
	OpenSSL_add_all_algorithms(); /* �������� SSL �㷨 */
	SSL_load_error_strings();/* �������� SSL ������Ϣ */
	ctx = SSL_CTX_new(SSLv23_server_method()); /* �� SSL V2 �� V3 ��׼���ݷ�ʽ����һ�� SSL_CTX ���� SSL Content Text *//* Ҳ������ SSLv2_server_method() �� SSLv3_server_method() ������ʾ V2 �� V3��׼ */
	if (ctx == NULL)
	{
		ERR_print_errors_fp(stdout);
		system("pause");
		exit(1);
	}
	if (SSL_CTX_use_certificate_file(ctx, "cacert.pem", SSL_FILETYPE_PEM) <= 0) /* �����û�������֤�飬 ��֤���������͸��ͻ��ˡ� ֤��������й�Կ */
	{
		ERR_print_errors_fp(stdout);
		system("pause");
		exit(1);
	}
	//SSL_CTX_set_default_passwd_cb_userdata(ctx,PRIKEY_CODE); //������ʹ�õĿ�˽���ļ�����û����һ�д��룬�����֡�EnterPEM pess phrass:��---��������  
	if (SSL_CTX_use_PrivateKey_file(ctx, "privatekey.pem", SSL_FILETYPE_PEM) <= 0) /* �����û�˽Կ */
	{
		ERR_print_errors_fp(stdout);
		system("pause");
		exit(1);
	}
	if (!SSL_CTX_check_private_key(ctx)) /* ����û�˽Կ�Ƿ���ȷ */
	{
		ERR_print_errors_fp(stdout);
		system("pause");
		exit(1);
	}


	initialize();

	printf("��ʼ������ɣ�\n");
	printf("���ڼ����û�������\n");

	cnt = 0;
	SOCKADDR clntAddr;
	int temp = sizeof(clntAddr);
	while (cnt != 10000)
	{
		//printf("%d\n",GetCurrentThreadId());
		SOCKET *clntSock = new SOCKET;
		*clntSock = accept(servSock, (SOCKADDR*)&clntAddr, &temp);
		printf("һ�û�������\n");
		cnt++;
		if (clntSock <= 0)
		{
			printf("error accept!");
			break;
		}
		_beginthreadex(NULL, 0, thread_run, clntSock, 0, NULL);
		//��ͻ��˷�������
	}
	if (cnt == 10000)
		printf("�����û�������\n");
	closesocket(servSock);


	free(matrix);
	free(bitmatrix);

	for (int i = 0; i < ramp_k*ramp_m*ramp_w*ramp_w + 1; i++)
		free(schedule[i]);
	free(schedule);
	//��ֹ DLL ��ʹ��
	WSACleanup();
	system("pause");
	return 0;
}

unsigned int __stdcall  thread_run(void *sock)
{
	SOCKET tSock = *((SOCKET *)sock);
	delete sock;
	int sendTimeout = 10000;
	setsockopt(tSock, SOL_SOCKET, SO_SNDTIMEO, (char *)&sendTimeout, sizeof(int));
	int recvTimeout = 3600000;
	setsockopt(tSock, SOL_SOCKET, SO_RCVTIMEO, (char *)&recvTimeout, sizeof(int));


	SSL *ssl = SSL_new(ctx);/* ���� ctx ����һ���µ� SSL */
	SSL_set_fd(ssl, tSock);/* �������û��� socket ���뵽 SSL */
	if (SSL_accept(ssl) == -1) /* ���� SSL ���� */
	{
		perror("ssl δ����\n");
		closesocket(tSock);
		return -1;
	}


	char f[1];
	while (true)
	{
		int recnum = SSL_read(ssl, f, 1);
		if (recnum <= 0)
			break;
		if (f[0] == SIGN_IN)
		{
			User *user = sign_in(ssl);
			if (user == NULL)
				continue;
			else
				user->handleClnt();
			delete user;
			break;
		}
		else if (f[0] == SIGN_UP)
		{
			User *user = sign_up(ssl);
			if (user == NULL)
				continue;
			else
				user->handleClnt();
			break;
		}
		else if (f[0] == UPLOAD_FILE)
		{
			char cookies[33];
			int readnum = SSL_read(ssl, cookies, 32);
			while (readnum != 32)
			{
				if (readnum <= 0)
				{
					break;
				}
				readnum += SSL_read(ssl, cookies + readnum, 32 - readnum);
			}
			cookies[32] = 0;
			char *hexcookies = charto16(cookies, 33);
			if (all_users.count(hexcookies) == 0)
			{
				printf("�Ƿ����ӣ�\n");
				SSL_shutdown(ssl);/* �ر� SSL ���� */
				SSL_free(ssl); /* �ͷ� SSL */
				closesocket(tSock);
				ExitThread(0);
				return 0;
			}
			int ret = all_users[hexcookies]->upload_files(ssl);
			if (ret == 0)
			{
				printf("�û��ϴ��ļ��Ƿ����ϴ����ޣ�\n");
				SSL_shutdown(ssl);/* �ر� SSL ���� */
				SSL_free(ssl); /* �ͷ� SSL */
				closesocket(tSock);
				ExitThread(0);
				return 0;
			}
			else if (ret == -1)
			{
				printf("�����ж�!\n");
				SSL_shutdown(ssl);/* �ر� SSL ���� */
				SSL_free(ssl); /* �ͷ� SSL */
				closesocket(tSock);
				ExitThread(0);
				return 0;
			}
			else
			{
				SSL_shutdown(ssl);/* �ر� SSL ���� */
				SSL_free(ssl); /* �ͷ� SSL */
				closesocket(tSock);
				ExitThread(0);
				return 0;
			}
		}
		else if (f[0] == DOWNLOAD_FILE)
		{
			char cookies[33];
			int readnum = SSL_read(ssl, cookies, 32);
			while (readnum != 32)
			{
				if (readnum <= 0)
				{
					break;
				}
				readnum += SSL_read(ssl, cookies + readnum, 32 - readnum);
			}
			cookies[32] = 0;
			char *hexcookies = charto16(cookies, 33);
			if (all_users.count(hexcookies) == 0)
			{
				printf("�Ƿ����ӣ�\n");
				SSL_shutdown(ssl);/* �ر� SSL ���� */
				SSL_free(ssl); /* �ͷ� SSL */
				closesocket(tSock);
				ExitThread(0);
				return 0;
			}
			int ret = all_users[hexcookies]->download_file(ssl);
			if (ret == 0)
			{
				printf("�û������ļ��Ƿ����ϴ����ޣ�\n");
				SSL_shutdown(ssl);/* �ر� SSL ���� */
				SSL_free(ssl); /* �ͷ� SSL */
				closesocket(tSock);
				ExitThread(0);
				return 0;
			}
			else if (ret == -1)
			{
				printf("�����ж�!\n");
				SSL_shutdown(ssl);/* �ر� SSL ���� */
				SSL_free(ssl); /* �ͷ� SSL */
				closesocket(tSock);
				ExitThread(0);
				return 0;
			}
			else
			{
				SSL_shutdown(ssl);/* �ر� SSL ���� */
				SSL_free(ssl); /* �ͷ� SSL */
				closesocket(tSock);
				ExitThread(0);
				return 0;
			}
		}
		else
		{
			SSL_shutdown(ssl);/* �ر� SSL ���� */
			SSL_free(ssl); /* �ͷ� SSL */
			printf("�Ƿ����ӣ�\n");
			closesocket(tSock);
			ExitThread(0);
			return 0;
		}
	}
	SSL_shutdown(ssl);/* �ر� SSL ���� */
	SSL_free(ssl); /* �ͷ� SSL */
	closesocket(tSock);
	printf("һ���û�������!\n");
	cnt--;
	ExitThread(0);
	return 0;
}

void initialize()
{
	//��֤���е��ļ��ж�������
	char fname[256];
	for (int i = 0; i < ramp_k; i++)
	{
		sprintf_s(fname, 100, "file_k_%d", i);
		if (_access(fname, 0) == -1)
		{
			_mkdir(fname);
		}
	}
	for (int i = 0; i < ramp_m; i++)
	{
		sprintf_s(fname, 100, "file_m_%d", i);
		if (_access(fname, 0) == -1)
		{
			_mkdir(fname);
		}
	}

	
}