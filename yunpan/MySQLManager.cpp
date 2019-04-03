#include "MySQLManager.h"    


MySQLManager::MySQLManager(char * hosts, char * userName, char * password, char * dbName)
{
	mySQLClient = mysql_init(NULL);//    初始化相关对象    
	if (!mysql_real_connect(mySQLClient, hosts, userName, password, dbName, 0, NULL, 0))
	{//    连接到服务器
		std::cout << "Error connection to database: \n" << mysql_error(mySQLClient) << std::endl;
	}
	char *sql = "create database if not exists yunpan;";
	if (mysql_real_query(mySQLClient, sql, (unsigned int)strlen(sql)) == 0){
		sql = "use yunpan;";
		mysql_real_query(mySQLClient, sql, (unsigned int)strlen(sql));
		sql = "create table if not exists users(usernamehash char(66) unique,userpassword char(65),username text,userpublickey char(132) unique,status int,primary key(usernamehash));";
		mysql_real_query(mySQLClient, sql, (unsigned int)strlen(sql));
		sql = "create table if not exists allfiles(fileID char(66) unique,	location text,filelen int,primary key(fileID));";
		mysql_real_query(mySQLClient, sql, (unsigned int)strlen(sql));
		sql = "set names utf8;";
		mysql_real_query(mySQLClient, sql, (unsigned int)strlen(sql));
	}
	else
	{
		printf("数据库出错\n");
	}

	char value = 1;
	mysql_options(mySQLClient, MYSQL_OPT_RECONNECT, (char *)&value);
	//以下都是初始化参数化查询的部分

	//写入某人
	sql = "insert users value(?,?,?,?,1);";
	person_stmt = mysql_stmt_init(mySQLClient);
	mysql_stmt_prepare(person_stmt, sql, (unsigned int)strlen(sql));
	
	memset(person_bind, 0, sizeof(person_bind));
	person_bind[0].buffer_type = MYSQL_TYPE_STRING;
	person_bind[0].buffer_length = 65;	
	person_bind[1].buffer_type = MYSQL_TYPE_STRING;
	person_bind[1].buffer_length = 65;	

	person_bind[2].buffer_type = MYSQL_TYPE_BLOB;				//需要设置大小

	person_bind[3].buffer_type = MYSQL_TYPE_STRING;
	person_bind[3].buffer_length = 131;	
	

	//由于数据库名不能确定，因此不能直接预备好，只能用的时候预备
	file_stmt = mysql_stmt_init(mySQLClient);
	memset(file_bind, 0, sizeof(file_bind));
	file_bind[0].buffer_length = sizeof(int);
	file_bind[0].buffer_type = MYSQL_TYPE_LONG;
	file_bind[1].buffer_length = 65;
	file_bind[1].buffer_type = MYSQL_TYPE_STRING;
	file_bind[2].buffer_length = sizeof(int);
	file_bind[2].buffer_type = MYSQL_TYPE_LONG;
	file_bind[3].buffer_length = sizeof(int);
	file_bind[3].buffer_type = MYSQL_TYPE_LONG;

	file_bind[4].buffer_type = MYSQL_TYPE_BLOB;				//需要设置大小

	file_bind[5].buffer_length = sizeof(int);
	file_bind[5].buffer_type = MYSQL_TYPE_LONG;
	file_bind[6].buffer_length = 259;
	file_bind[6].buffer_type = MYSQL_TYPE_STRING;
	file_bind[7].buffer_length = sizeof(int);
	file_bind[7].buffer_type = MYSQL_TYPE_LONG;

	lock=CreateMutex(NULL, FALSE, NULL);
	printf("数据库初始化完成！\n");

}

MySQLManager::~MySQLManager()
{
	mysql_stmt_close(person_stmt);
	mysql_stmt_close(file_stmt);
	mysql_close(mySQLClient);

	CloseHandle(lock);
}

//直接运行sql语句
MYSQL_RES *MySQLManager::runSQL(const char *sql)
{
	WaitForSingleObject(lock, INFINITE);
	mysql_ping(mySQLClient);
	if (mysql_real_query(mySQLClient, sql, (unsigned int)strlen(sql)) != 0)
	{
		cout << sql<<endl;
		cout << mysql_error(mySQLClient) << endl;
		printf("数据库出错\n");
	}
	MYSQL_RES *res= mysql_store_result(mySQLClient);
	ReleaseMutex(lock);
	return res;
}

//插入好友，name是自己的hash，friendname是好友的hash
bool MySQLManager::friendInsert(char *name, char *friendname)
{
	char *temp = charto16(friendname, 33);
	string sql = "select usernamehash,username,userpublickey from users where usernamehash='" + string(temp) + "';";
	MYSQL_RES *res = runSQL(sql.c_str());
	free(temp);
	MYSQL_ROW row;
	if (row = mysql_fetch_row(res))
	{
		temp = charto16(name, 33);
		sql = "insert friend" + string(temp+6) + " values('" + string(row[0]) + "','" + string(row[1]) + "','" + string(row[2]) + "');";
		mysql_free_result(runSQL(sql.c_str()));
		free(temp);
		mysql_free_result(res);
		return true;
	}
	else
	{
		mysql_free_result(res);
		return false;
	}
}

//从某人删除好友
bool MySQLManager::friendDelete(char *name, char *friendname)
{
	char *temp1 = charto16(name, 33);
	char *temp2 = charto16(friendname, 33);
	string sql = "delete from friend" + string(temp1+6) + " where friendid='" + string(temp2) + "';";
	MYSQL_RES *res = runSQL(sql.c_str());
	free(temp1);
	free(temp2);
	if (mysql_fetch_row(res)!=NULL)
	{
		mysql_free_result(res);
		return true;
	}
	else
	{
		mysql_free_result(res);
		return false;
	}
}

//得到某个人的公钥，buffer返回give_friend或failed_action
bool MySQLManager::getPerson(char *namehash, char **buffer)
{
	char *temp = charto16(namehash, 33);
	string sql = "select userpublickey  from users where usernamehash='" + string(temp) + "';";
	MYSQL_RES *res = runSQL(sql.c_str());

	if (res==NULL)
	{
		*buffer = new char[1];
		(*buffer)[0] = FAILED_ACTION;
		mysql_free_result(res);
		return false;
	}

	MYSQL_ROW row;
	free(temp);
	if (row=mysql_fetch_row(res))
	{
		*buffer = new char[65 + 1];
		(*buffer)[0] = GIVE_FRIEND;
		hextochar(row[0], *buffer + 1, 65);
		mysql_free_result(res);
		return true;
	}
	else
	{
		*buffer = new char[1];
		(*buffer)[0] = FAILED_ACTION;
		mysql_free_result(res);
		return false;
	}
}

//要传入用户名哈希，loc为位置，也就是父节点的标号，也就是数据库中的id，deep表示第几层
//返回buffer的大小
//发送文件列表数据帧格式：标志（1）|文件个数（4）|id（4）|文件id（32）|文件名长度（1）|文件名（*）|文件大小（4）|文件公钥（129）|文件类型（1）|*
int MySQLManager::getFiles(char *name, int loc, int deep, char** buffer)
{
	char *temp = charto16(name, 33);
	string sql = "select id,fileid,filename,filesize,filekey,type from file" + string(temp+4) + "where loc=" + to_string(deep) + " and father=" + to_string(loc) + ";";
	//cout << sql << endl;
	MYSQL_RES *res = runSQL(sql.c_str());
	MYSQL_ROW row;
	free(temp);
	if (res == NULL)
	{
		*buffer = new char[5];
		(*buffer)[0] = GIVE_FILES;
		int_to_bytes(0, (unsigned char *)*buffer + 1);
		mysql_free_result(res);
		return 5;
	}
	int rownum = mysql_num_rows(res);
	if (rownum == 0)
	{
		*buffer = new char[5];
		(*buffer)[0] = GIVE_FILES;
		int_to_bytes(0, (unsigned char *)*buffer + 1);
		return 5;
	}
	int *id=new int[rownum];
	char **fileid = new char*[rownum];
	char **filename = new char*[rownum];
	int *filesize = new int[rownum];
	char **filekey = new char*[rownum];
	int *type = new int[rownum];
	int alllen = 5;
	for (int i = 0; i < rownum; i++)
	{
		row = mysql_fetch_row(res);
		sscanf_s(row[0], "%d", id + i);
		fileid[i] = row[1];
		filename[i] = row[2];
		sscanf_s(row[3], "%d", filesize + i);
		filekey[i] = row[4];
		sscanf_s(row[5], "%d", type + i);
		alllen += 4 + 32 + strlen(row[2])+1 + 4 + 129+1;
	}
	*buffer = new char[alllen];
	(*buffer)[0] = GIVE_FILES;
	int_to_bytes(rownum, (unsigned char *)*buffer + 1);
	int l = 5;
	for (int i = 0; i < rownum; i++)
	{
		int_to_bytes(id[i], (unsigned char *)*buffer + l);
		l += 4;
		hextochar(fileid[i], *buffer + l, 32);
		l += 32;
		(*buffer)[l] = strlen(filename[i]);
		l += 1;
		strcpy_s(*buffer + l, strlen(filename[i])+1, filename[i]);
		l += (unsigned char)(*buffer)[l - 1];
		int_to_bytes(filesize[i], (unsigned char *)*buffer + l);
		l += 4;
		hextochar(filekey[i], *buffer + l, 129);
		l += 129;
		(*buffer)[l] = type[i];
		l += 1;
	}
	delete[] id;
	delete[] fileid;
	delete[] filename;
	delete[] filesize;
	delete[] filekey;
	delete[] type;
	mysql_free_result(res);
	//用于后期debug
	if (alllen != l)
	{
		system("pause");
	}
	return alllen;
}

//返回buffer的大小
//发送文件列表数据帧格式:标志（1）|好友个数（4）|好友id（32）|好友名大小（1）|好友名（*）|好友公钥（65）|*
int MySQLManager::getFriends(char *name, char** buffer)
{
	char *temp = charto16(name, 33);
	string sql = "select friendid,friendname,friendkey from friend" + string(temp+6)+";";
	MYSQL_RES *res = runSQL(sql.c_str());
	MYSQL_ROW row;
	free(temp);
	if (res == NULL)
	{
		*buffer = new char[5];
		(*buffer)[0] = GIVE_FRIENDS;
		int_to_bytes(0, (unsigned char *)*buffer + 1);
		mysql_free_result(res);
		return 5;
	}
	int rownum = mysql_num_rows(res);
	if (rownum == 0)
	{
		*buffer = new char[5];
		(*buffer)[0] = GIVE_FRIENDS;
		int_to_bytes(0, (unsigned char *)*buffer + 1);
		mysql_free_result(res);
		return 5;
	}
	char **friendid = new char*[rownum];
	char **friendname = new char*[rownum];
	char **friendkey = new char*[rownum];
	int alllen = 5;
	for (int i = 0; i < rownum; i++)
	{
		row = mysql_fetch_row(res);
		friendid[i] = row[0];
		friendname[i] = row[1];
		friendkey[i] = row[2];
		alllen += 32 + strlen(row[1])+1 +65;
	}
	*buffer = new char[alllen];
	(*buffer)[0] = GIVE_FRIENDS;
	int_to_bytes(rownum, (unsigned char *)*buffer + 1);
	int l = 5;
	for (int i = 0; i < rownum; i++)
	{
		hextochar(friendid[i], *buffer + l, 32);
		l += 32;
		(*buffer)[l] = strlen(friendname[i]);
		l += 1;
		strcpy_s(*buffer + l, strlen(friendname[i])+1, friendname[i]);
		l += (unsigned char)(*buffer)[l - 1];
		hextochar(friendkey[i], *buffer + l, 65);
		l += 65;
	}
	delete[] friendid;
	delete[] friendname;
	delete[] friendkey;
	mysql_free_result(res);
	//用于后期debug
	if (alllen != l)
	{
		system("pause");
	}
	return alllen;
}

//文件是否存在
bool MySQLManager::fileExist(char *name)
{
	char *temp = charto16(name, 33);
	string sql = "select fileID from allfiles where fileID='" + string(temp) + "';";
	MYSQL_RES *res = runSQL(sql.c_str());
	free(temp);
	if (res==NULL)
	{
		mysql_free_result(res);
		return false;
	}
	if (mysql_fetch_row(res) == NULL)
	{
		mysql_free_result(res);
		return false;
	}
	else
	{
		mysql_free_result(res);
		return true;
	}
}

//用户是否存在
bool MySQLManager::personExist(char *name)
{
	char *temp = charto16(name, 33);
	string sql = "select usernamehash from users where usernamehash='" + string(temp) + "';";
	MYSQL_RES *res = runSQL(sql.c_str());
	free(temp);
	if (res == NULL)
	{
		return false;
	}
	if (mysql_fetch_row(res) == NULL)
	{
		mysql_free_result(res);
		return false;
	}
	else
	{
		mysql_free_result(res);
		return true;
	}
}

//用于对用户存储的某一层的文件进行紧缩操作
//由于紧缩操作代价很大，需要进行某一层文件id的全部更改，需要进行大量数据库操作，因此要尽量避免，只有当迫不得已的时候才能使用
bool MySQLManager::cutID(char *name, int deep)
{
	char *temp = charto16(name, 33);
	//先找到当前这级目录下的最大的id标号
	string sql = "select id from file" + string(temp+4) + " where loc=" + to_string(deep) + ";";
	MYSQL_RES *res = runSQL(sql.c_str());
	if (res == NULL)
	{
		free(temp);
		return false;
	}
	MYSQL_ROW row;
	int realid = 1;
	int id;
	while (row = mysql_fetch_row(res))
	{
		sscanf_s(row[0], "%d", &id);
		if (id == realid)
			continue;
		sql = "update file" + string(temp+4) + " set id=" + to_string(realid) + " where loc=" + to_string(deep) + " and id=" + to_string(id) + ";";
		mysql_free_result(runSQL(sql.c_str()));
		sql = "update file" + string(temp+4) + " set father=" + to_string(realid) + " where loc=" + to_string(deep+1) + " and father=" + to_string(id) + ";";
		mysql_free_result(runSQL(sql.c_str()));
		realid++;
	}
	mysql_free_result(res);
	free(temp);
	return true;
}

//向某人写入某文件
//数据库中的文件结构利用id实现了一个树状结构，每个节点保存他在第几层和它的父节点id，以及他自己的id
//当期存储结构存在一个问题，就是文件的id是在当前层来说的，用的int类型限制了某一层的文件数目最多有21亿，实际使用过程中有可能会超过，因为这是这一层的所有的文件
//一个直接的解决办法就是限制用户上传的文件数目，或是扩大int的范围，这里使用了限制用户上传的数目来实现，限制用户在当前层上传最多2亿条文件
//树状结构：id|fileid|loc|father|filename|filesize|filekey|type
//直接选择最大的id+1作为新写入的当前文件的值，会造成id存储的稀疏化，稀疏化达到一定程度需要进行紧缩操作
int MySQLManager::writeFile(char *username,char *filename, int namelen, int filelen, char *fileID, char *fileKey, int loc, int locdeep,int type)
{
	char *temp = charto16(username, 33);
	//先找到当前这级目录下的最大的id标号
	string sql = "select max(id),count(id) from file" + string(temp+4) + " where loc=" + to_string(locdeep) + ";";
	MYSQL_RES *res = runSQL(sql.c_str());
	MYSQL_ROW row;
	if (row=mysql_fetch_row(res))
	{
		WaitForSingleObject(lock, INFINITE);
		mysql_ping(mySQLClient);
		sql = "insert file" + string(temp + 4) + " values(?,?,?,?,?,?,?,?);";
		file_stmt = mysql_stmt_init(mySQLClient);
		mysql_stmt_prepare(file_stmt, sql.c_str(), (unsigned int)strlen(sql.c_str()));
		int id ;
		int cnt;
		if (row[0] == NULL)
			id = 0;
		else
			sscanf_s(row[0],"%d",&id);
		sscanf_s(row[1], "%d", &cnt);;
		id++;
		//限制用户在当前层上传的文件数目最多为两亿
		if (cnt >= 200000000)
		{
			mysql_free_result(res);
			free(temp);
			return false;
		}

		char *temp1 = charto16(fileID, 33);
		char *temp2 = charto16(fileKey, 130);
		file_bind[0].buffer = &id;
		file_bind[1].buffer = temp1;
		file_bind[2].buffer = &locdeep;
		file_bind[3].buffer = &loc;
		file_bind[4].buffer_length = namelen + 1;
		file_bind[4].buffer = filename;
		file_bind[5].buffer = &filelen;
		file_bind[6].buffer = temp2;
		file_bind[7].buffer = &type;
		mysql_stmt_bind_param(file_stmt, file_bind);	//要绑定输入缓冲区
		if (mysql_stmt_execute(file_stmt) == 0)
		{
			ReleaseMutex(lock);
			mysql_free_result(res);
			free(temp);
			free(temp1);
			free(temp2);
			//超过阀值才要进行紧缩操作
			//将阀值设为文件数目达到100万，且id达到文件条数的10倍
			if (cnt >= 1000000 && id > cnt * 10)
			{
				cutID(username, loc);
			}
			return id;
		}
		else
		{
			cout << mysql_stmt_error(file_stmt) << endl;
			ReleaseMutex(lock);
			mysql_free_result(res);
			free(temp);
			free(temp1);
			free(temp2);
			return 0;
		}
		
	}
	else
	{
		mysql_free_result(res);
		free(temp);
		return 0;
	}
}

//从某人删除某文件
bool MySQLManager::deleteFile(char *name, int id, int deep)
{
	char *temp = charto16(name, 33);
	string sql = "select id,loc from file" + string(temp+4) + " where father=" + to_string(id) + " and loc=" + to_string(deep+1) + ";";
	MYSQL_RES *res = runSQL(sql.c_str());
	if (res == NULL)
	{
		free(temp);
		return false;
	}
	MYSQL_ROW row;
	while(row = mysql_fetch_row(res))
	{
		int nextid, loc;
		sscanf_s(row[0], "%d", &nextid);
		sscanf_s(row[1], "%d", &loc);
		deleteFile(name, nextid, loc);
	}
	mysql_free_result(res);
	sql = "delete from file" + string(temp+4) + " where id=" + to_string(id) + " and loc=" + to_string(deep) + ";";
	res = runSQL(sql.c_str());
	mysql_free_result(res);
	free(temp);
	return true;
}

//向系统有的文件中写入某文件
bool MySQLManager::writeFile(int filelen, char *fileID)
{
	char *temp;
	temp = charto16(fileID, 33);
	temp[64] = 0;
	string sql = "insert allfiles values('" + string(temp) + "',\'\','" + to_string(filelen) + "')";
	MYSQL_RES *res = runSQL(sql.c_str());
	free(temp);
	mysql_free_result(res);
	return true;
}

//文件是否是用户的,返回文件的大小
int MySQLManager::fileIsPerson(char *filename, char *name)
{
	char *temp1 = charto16(name, 33);
	char *temp2 = charto16(filename, 33);
	string sql = "select filesize from file" + string(temp1+4)+ string(" where fileid='") + string(temp2) + "'";
	MYSQL_RES *res = runSQL(sql.c_str());
	MYSQL_ROW row;
	free(temp1);
	free(temp2);
	if (res == NULL)
	{
		return -1;
	}
	if (row=mysql_fetch_row(res))
	{
		int size;
		sscanf_s(row[0], "%d", &size);
		mysql_free_result(res);
		return size;
	}
	else
	{
		mysql_free_result(res);
		return -1;
	}
}

//某人是否是某人的好友
bool MySQLManager::personIsFriend(char *user, char *friendname)
{
	char *temp1 = charto16(user, 33);
	char *temp2 = charto16(friendname, 33);
	string sql = "select friendid from friend" + string(temp1+6) + " where friendid ='" + string(temp2) + "';";
	MYSQL_RES *res = runSQL(sql.c_str());
	free(temp1);
	free(temp2);
	if (res == NULL)
	{
		return false;
	}
	if (mysql_fetch_row(res) != NULL)
	{
		mysql_free_result(res);
		return true;
	}
	else
	{
		mysql_free_result(res);
		return false;
	}
}

//用户登录
bool MySQLManager::personLogin(char *name, char *password)
{
	char *temp1 = charto16(name, 33);
	char *temp2 = charto16(password, 33);
	//同时满足用户名和密码正确还状态为0，没登录
	string sql = "select status from users where usernamehash ='" + string(temp1) + "'and userpassword ='" + string(temp2) + "'and status=0;";
	MYSQL_RES *res = runSQL(sql.c_str());
	if (res == NULL)
	{
		return false;
	}
	if (mysql_fetch_row(res)!=NULL)
	{
		//要及时修改状态
		mysql_free_result(res);
		sql = "update users set status=1 where usernamehash ='" + string(temp1) + "';";
		res = runSQL(sql.c_str());
		mysql_free_result(res);
		return true;
	}
	else
	{
		mysql_free_result(res);
		return false;
	}
}

//用户登出
bool MySQLManager::personLogout(char *name)
{
	char *temp = charto16(name, 33);
	string sql = "update users set status=0 where usernamehash ='" + string(temp) + "';";
	MYSQL_RES *res = runSQL(sql.c_str());
	free(temp);
	if (res == NULL)
	{
		return false;
	}
	if (mysql_fetch_row(res) == NULL)
	{
		mysql_free_result(res);
		return false;
	}
	else
	{
		mysql_free_result(res);
		return true;
	}
}

//插入用户，也就是用户注册
bool MySQLManager::personInsert(char *userid, char *password, char *pkey, char *name, int namelen)
{
	//先要创建表
	char *table = charto16(userid, 33);
	string sql = "create table if not exists file" + string(table+4) +
		"(id int,fileid char(65),loc int,father int,filename text,filesize int,filekey text(260),type int)";
	mysql_free_result(runSQL(sql.c_str()));
	sql = "create table if not exists friend" + string(table+6) +
		"(friendid char(65) unique,friendname text,friendkey char(132))";
	mysql_free_result(runSQL(sql.c_str()));
	char *temp1 = charto16(password, 33);
	char *temp2 = charto16(pkey, 66);
	//然后是参数化插入防名字部分注入

	person_bind[0].buffer = table;
	person_bind[1].buffer = temp1;
	person_bind[2].buffer = name;
	person_bind[2].buffer_length = namelen + 1;
	person_bind[3].buffer = temp2;
	mysql_stmt_bind_param(person_stmt, person_bind);	//要绑定输入缓冲区
	WaitForSingleObject(lock, INFINITE);
	if (mysql_stmt_execute(person_stmt) != 0)
	{
		ReleaseMutex(lock);
		free(table);
		free(temp1);
		free(temp2);
		return true;
	}
	else
	{
		ReleaseMutex(lock);
		free(table);
		free(temp1);
		free(temp2);
		return false;
	}
}



