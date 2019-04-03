#ifndef MYSQLMANAGER_H_    
#define MYSQLMANAGER_H_     

#include"user.h"
#include <mysql.h>
#include <string>    
#include <iostream>    
#include <map>    
#include<Windows.h>
#include <string.h>    

using namespace std;


//���жԱ�ĸ�������룬�½�������û����֤�Ƿ�ɹ�����ʱ��Ҫ���������
class MySQLManager
{
public:
	
	MySQLManager(char * hosts, char * userName, char * password, char * dbName);
	~MySQLManager();
	
	bool friendInsert(char *name, char *friendname);
	bool friendDelete(char *name, char *friendname);

	bool getPerson(char *namehash, char **buffer);
	int getFiles(char *name, int loc,int deep,char** buffer);
	int getFriends(char *name, char **buffer);
	bool fileExist(char *name);
	bool personExist(char *name);
	int writeFile(char *username,char *filename,int namelen,int filelen,char *fileID,char *fileKey,int loc,int locdeep,int type );
	bool writeFile(int filelen, char *fileID);
	bool deleteFile(char *name,int id,int deep);
	int fileIsPerson(char *filename, char *name);
	bool personIsFriend(char *user,char *friendname);
	bool personLogin(char *name,char *password);

	bool personInsert(char *userid, char *password, char *pkey,char *name,int namelen);
	bool personLogout(char *name);

private:
	MYSQL *mySQLClient;
	HANDLE lock;
	//������Ҫд�����ݿ�ĳ���writefile��personinsert��Ϊ���ַ��⣬����������Ҫ���в���������Ϊ���ǵ����붼�����hex����
	//д��ĳ��
	MYSQL_BIND person_bind[4];
	MYSQL_STMT *person_stmt;

	//��ĳ��д��ĳ�ļ�
	MYSQL_BIND file_bind[8];
	MYSQL_STMT *file_stmt;


	bool cutID(char *name, int deep);

	MYSQL_RES *runSQL(const char *sql);

};

#endif /* MYSQLMANAGER_H_ */ 