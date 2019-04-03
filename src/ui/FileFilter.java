package ui;

import java.io.File;

import javax.swing.filechooser.FileFilter;

class FileFilterOfDocument extends FileFilter {
	@Override
	public boolean accept(File f) {
		// TODO Auto-generated method stub
		return f.getName().endsWith("txt")||f.getName().endsWith("doc")||f.getName().endsWith("docx")||f.getName().endsWith("xls")||
				f.getName().endsWith("xlsx")||f.getName().endsWith("pdf")||f.getName().endsWith("ppt")||f.getName().endsWith("pptx")||
				f.isDirectory();
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return "Œƒµµ(txt,doc,docx,xls,xlsx,pdf,ppt,pptx)";
	}
	
}

class FileFilterOfMovie extends FileFilter {
	@Override
	public boolean accept(File f) {
		// TODO Auto-generated method stub
		return f.getName().endsWith("mp4")||f.getName().endsWith("avi")||f.getName().endsWith("flv")||
				f.getName().endsWith("mkv")||f.getName().endsWith("rmvb")||f.isDirectory();
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return " ”∆µ(mp4,mkv,rmvb,avi,flv)";
	}
	
}

class FileFilterOfMusic extends FileFilter {
	@Override
	public boolean accept(File f) {
		// TODO Auto-generated method stub
		return f.getName().endsWith("mp3")||f.getName().endsWith("wav")
				||f.getName().endsWith("m4a")||f.isDirectory();
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return "“Ù∆µ(mp3,wav,m4a)";
	}
	
}


class FileFilterOfPicture extends FileFilter {
	@Override
	public boolean accept(File f) {
		// TODO Auto-generated method stub
		return f.getName().endsWith("jpg")||f.getName().endsWith("png")||f.getName().endsWith("ico")||
				 f.getName().endsWith("bmp")||f.getName().endsWith("gif")||f.isDirectory();
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return "Õº∆¨(jpg,png,ico,bmp,gif)";
	}
	
}

