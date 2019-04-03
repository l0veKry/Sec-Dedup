//#pragma once
#ifndef USER_H_    
#define USER_H_    

#include <cstring>
#include <cstdio>
#include <cstdlib>
#include<ctime>
#include<iostream>
#include<string>
#include<map>




using namespace std;

extern "C"
{
#include<io.h>
#include<direct.h>
#include<time.h>
#include<winsock2.h>
#include <process.h>  
#include <windows.h> 
#include "jerasure.h"
#include "reed_sol.h"
#include "galois.h"
#include "cauchy.h"
#include "liberation.h"


#include <openssl/rsa.h>       /* SSLeay stuff */  
#include <openssl/crypto.h>  
#include <openssl/x509.h>  
#include <openssl/pem.h>  
#include <openssl/ssl.h>  
#include <openssl/err.h> 
}

#define BUFFERSIZE (ramp_k*blocksize)

#define SIGN_IN 1
#define SIGN_UP 3
#define SIGN_OUT 2
#define REQUIRE_FILES 4
#define REQUIRE_FRIENDS 5
#define UPLOAD_FILE 7
#define DOWNLOAD_FILE 8
#define SHARE_FILE 10
#define ADD_FRIEND	9
#define CREATE_DIR 11
#define DELETE_FILE 12


#define FAILED_ACTION 100
#define SUCCESS_LOGN 101
#define GIVE_FILES 102
#define GIVE_FRIENDS 103
//#define DEMMAND_FILE 104
#define SEND_FILE 105
#define GIVE_FRIEND 106
#define UPLOAD_FINISH 107
#define SUCCESS_SHARE 108
#define SUCCESS_CREATE 109;
#define SUCCESS_DELETE 110;




int bytes_to_int(unsigned char *b);

void int_to_bytes(int i, unsigned char *b);

void randomiv(char *iv);

//size的大小可以举个例子，如32个字节的id，size应为33
char *charto16(char *s,int size);

void hextochar(char *src,char *des,int size);

class User{
private:
	char *userid;
	SSL * clntSock;
	char *userpassword;

	//恢复文件
	void recover_file(FILE **fd, FILE **fs, int *erase, int *erasures, int blocknum);

	//用户请求文件列表
	bool require_files();

	//用户请求好友列表
	int require_friends();

	int add_friend();

	int createDir();

	int deleteFile();

	//校验文件
	int check_file();

	//用户上传部分文件来进行校验
	bool upload_partfile(int filesize, char *fileID, SSL * upSock,int left, int right);

	bool check_partfile(SSL * clntSock, char *checkdata, int ct);

	//用户分享文件
	bool share_file();

	bool writeTreeFiles(char *friendID, int loc, int deep, int num);

	//向用户发送文件
	void send_file(SSL * clntSock, char *filename, int size);

	//接收用户上传的文件
	bool recvfile( int size,char *filename, SSL * upSock);

public:
	User(char *name,char *password,SSL * clntSock);
	~User();

	int sign_out();

	//用户处理
	void  handleClnt();


	//用户正式上传文件
	int upload_files(SSL * upSock);

	//用户下载文件
	int download_file(SSL * doSock);


	int check_cookies(char *getcookies);

};

User *sign_in(SSL * clntSock);
User *sign_up(SSL * clntSock);



#endif