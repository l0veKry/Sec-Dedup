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


//所有对表的更新如插入，新建操作都没有验证是否成功，有时间要补上这个坑
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
	//除了需要写入数据库的长的writefile和personinsert因为有字符外，其他都不需要进行参数化，因为他们的输入都会进行hex编码
	//写入某人
	MYSQL_BIND person_bind[4];
	MYSQL_STMT *person_stmt;

	//向某人写入某文件
	MYSQL_BIND file_bind[8];
	MYSQL_STMT *file_stmt;


	bool cutID(char *name, int deep);

	MYSQL_RES *runSQL(const char *sql);

};

#endif /* MYSQLMANAGER_H_ */ 