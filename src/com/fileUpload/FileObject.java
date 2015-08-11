package com.fileUpload;

import java.util.List;

public class FileObject {
	

	private String tag;
	
	
	//TODO: tag listesi tutulacak
	private List<String> tagList;
	
	private String fileName;
	
	

	public FileObject(String tag, String fileName) {
		this.tag = tag;
		this.fileName = fileName;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public List<String> getTagList() {
		return tagList;
	}

	public void setTagList(List<String> tagList) {
		this.tagList = tagList;
	}
	
	
	
	

}
