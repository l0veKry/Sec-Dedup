#include"user.h"
#include"MySQLManager.h"


#pragma comment(lib, "ws2_32.lib")
#pragma comment(lib,"C:\\Program Files\\MySQL\\MySQL Server 5.7\\lib\\libmysql.lib")
#pragma comment( lib, "libeay32.lib" )  
#pragma comment( lib, "ssleay32.lib" )  


extern map<string, User *> all_users;
//文件分为k份
extern int ramp_k;

//产生m块冗余码
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
////系统初始化
void initialize();
//
int cnt;
SSL_CTX *ctx;

//线程函数
unsigned int __stdcall  thread_run(void *sock);

int main()
{
	FILE *ini=NULL;
	fopen_s(&ini, "set1.ini", "r");
	if (ini == NULL)
	{
		printf("没有找到默认配置文件！");
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
	//printf("输入ip地址：");
	fscanf_s(ini,"[ip]%s", address, 20);
	//printf("输入端口号：");
	fscanf_s(ini, " [port]%d", &port);
	//printf("输入数据分块数k，冗余块数m：");
	fscanf_s(ini, " [k]%d[m]%d", &ramp_k, &ramp_m);

	//printf("输入数据库ip地址,用户名和密码:");
	fscanf_s(ini," [mysql-ip] %s", sqladdr, 100);
	fscanf_s(ini," [mysql-u] %s", name, 100);
	fscanf_s(ini," [mysql-p] %s", password, 100);


	Mysql = new MySQLManager(sqladdr, name, password, "mysql");

	printf("系统初始化中···\n");

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
	memset(&sockAddr, 0, sizeof(sockAddr));  //每个字节都用0填充
	sockAddr.sin_family = PF_INET;  //使用IPv4地址
	sockAddr.sin_addr.s_addr = htonl(INADDR_ANY);;  //具体的IP地址
	sockAddr.sin_port = htons(port);  //端口
	if (bind(servSock, (SOCKADDR*)&sockAddr, sizeof(SOCKADDR)) < 0)
	{
		printf("bind error!\n");
		system("pause");
		exit(1);
	}
	//进入监听状态
	listen(servSock, 10000);


	//ssl设置
	SSL_library_init();/* SSL 库初始化 */
	OpenSSL_add_all_algorithms(); /* 载入所有 SSL 算法 */
	SSL_load_error_strings();/* 载入所有 SSL 错误消息 */
	ctx = SSL_CTX_new(SSLv23_server_method()); /* 以 SSL V2 和 V3 标准兼容方式产生一个 SSL_CTX ，即 SSL Content Text *//* 也可以用 SSLv2_server_method() 或 SSLv3_server_method() 单独表示 V2 或 V3标准 */
	if (ctx == NULL)
	{
		ERR_print_errors_fp(stdout);
		system("pause");
		exit(1);
	}
	if (SSL_CTX_use_certificate_file(ctx, "cacert.pem", SSL_FILETYPE_PEM) <= 0) /* 载入用户的数字证书， 此证书用来发送给客户端。 证书里包含有公钥 */
	{
		ERR_print_errors_fp(stdout);
		system("pause");
		exit(1);
	}
	//SSL_CTX_set_default_passwd_cb_userdata(ctx,PRIKEY_CODE); //若不是使用的空私密文件，且没有这一行代码，则会出现“EnterPEM pess phrass:”---输入密码  
	if (SSL_CTX_use_PrivateKey_file(ctx, "privatekey.pem", SSL_FILETYPE_PEM) <= 0) /* 载入用户私钥 */
	{
		ERR_print_errors_fp(stdout);
		system("pause");
		exit(1);
	}
	if (!SSL_CTX_check_private_key(ctx)) /* 检查用户私钥是否正确 */
	{
		ERR_print_errors_fp(stdout);
		system("pause");
		exit(1);
	}


	initialize();

	printf("初始化已完成！\n");
	printf("正在监听用户···\n");

	cnt = 0;
	SOCKADDR clntAddr;
	int temp = sizeof(clntAddr);
	while (cnt != 10000)
	{
		//printf("%d\n",GetCurrentThreadId());
		SOCKET *clntSock = new SOCKET;
		*clntSock = accept(servSock, (SOCKADDR*)&clntAddr, &temp);
		printf("一用户已连接\n");
		cnt++;
		if (clntSock <= 0)
		{
			printf("error accept!");
			break;
		}
		_beginthreadex(NULL, 0, thread_run, clntSock, 0, NULL);
		//向客户端发送数据
	}
	if (cnt == 10000)
		printf("连接用户已满！\n");
	closesocket(servSock);


	free(matrix);
	free(bitmatrix);

	for (int i = 0; i < ramp_k*ramp_m*ramp_w*ramp_w + 1; i++)
		free(schedule[i]);
	free(schedule);
	//终止 DLL 的使用
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


	SSL *ssl = SSL_new(ctx);/* 基于 ctx 产生一个新的 SSL */
	SSL_set_fd(ssl, tSock);/* 将连接用户的 socket 加入到 SSL */
	if (SSL_accept(ssl) == -1) /* 建立 SSL 连接 */
	{
		perror("ssl 未建立\n");
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
				printf("非法连接！\n");
				SSL_shutdown(ssl);/* 关闭 SSL 连接 */
				SSL_free(ssl); /* 释放 SSL */
				closesocket(tSock);
				ExitThread(0);
				return 0;
			}
			int ret = all_users[hexcookies]->upload_files(ssl);
			if (ret == 0)
			{
				printf("用户上传文件非法或上传受限！\n");
				SSL_shutdown(ssl);/* 关闭 SSL 连接 */
				SSL_free(ssl); /* 释放 SSL */
				closesocket(tSock);
				ExitThread(0);
				return 0;
			}
			else if (ret == -1)
			{
				printf("连接中断!\n");
				SSL_shutdown(ssl);/* 关闭 SSL 连接 */
				SSL_free(ssl); /* 释放 SSL */
				closesocket(tSock);
				ExitThread(0);
				return 0;
			}
			else
			{
				SSL_shutdown(ssl);/* 关闭 SSL 连接 */
				SSL_free(ssl); /* 释放 SSL */
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
				printf("非法连接！\n");
				SSL_shutdown(ssl);/* 关闭 SSL 连接 */
				SSL_free(ssl); /* 释放 SSL */
				closesocket(tSock);
				ExitThread(0);
				return 0;
			}
			int ret = all_users[hexcookies]->download_file(ssl);
			if (ret == 0)
			{
				printf("用户下载文件非法或上传受限！\n");
				SSL_shutdown(ssl);/* 关闭 SSL 连接 */
				SSL_free(ssl); /* 释放 SSL */
				closesocket(tSock);
				ExitThread(0);
				return 0;
			}
			else if (ret == -1)
			{
				printf("连接中断!\n");
				SSL_shutdown(ssl);/* 关闭 SSL 连接 */
				SSL_free(ssl); /* 释放 SSL */
				closesocket(tSock);
				ExitThread(0);
				return 0;
			}
			else
			{
				SSL_shutdown(ssl);/* 关闭 SSL 连接 */
				SSL_free(ssl); /* 释放 SSL */
				closesocket(tSock);
				ExitThread(0);
				return 0;
			}
		}
		else
		{
			SSL_shutdown(ssl);/* 关闭 SSL 连接 */
			SSL_free(ssl); /* 释放 SSL */
			printf("非法连接！\n");
			closesocket(tSock);
			ExitThread(0);
			return 0;
		}
	}
	SSL_shutdown(ssl);/* 关闭 SSL 连接 */
	SSL_free(ssl); /* 释放 SSL */
	closesocket(tSock);
	printf("一名用户已离线!\n");
	cnt--;
	ExitThread(0);
	return 0;
}

void initialize()
{
	//保证所有的文件夹都建立了
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