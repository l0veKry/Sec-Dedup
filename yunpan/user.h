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

//size�Ĵ�С���Ծٸ����ӣ���32���ֽڵ�id��sizeӦΪ33
char *charto16(char *s,int size);

void hextochar(char *src,char *des,int size);

class User{
private:
	char *userid;
	SSL * clntSock;
	char *userpassword;

	//�ָ��ļ�
	void recover_file(FILE **fd, FILE **fs, int *erase, int *erasures, int blocknum);

	//�û������ļ��б�
	bool require_files();

	//�û���������б�
	int require_friends();

	int add_friend();

	int createDir();

	int deleteFile();

	//У���ļ�
	int check_file();

	//�û��ϴ������ļ�������У��
	bool upload_partfile(int filesize, char *fileID, SSL * upSock,int left, int right);

	bool check_partfile(SSL * clntSock, char *checkdata, int ct);

	//�û������ļ�
	bool share_file();

	bool writeTreeFiles(char *friendID, int loc, int deep, int num);

	//���û������ļ�
	void send_file(SSL * clntSock, char *filename, int size);

	//�����û��ϴ����ļ�
	bool recvfile( int size,char *filename, SSL * upSock);

public:
	User(char *name,char *password,SSL * clntSock);
	~User();

	int sign_out();

	//�û�����
	void  handleClnt();


	//�û���ʽ�ϴ��ļ�
	int upload_files(SSL * upSock);

	//�û������ļ�
	int download_file(SSL * doSock);


	int check_cookies(char *getcookies);

};

User *sign_in(SSL * clntSock);
User *sign_up(SSL * clntSock);



#endif