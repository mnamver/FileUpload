package com.fileUpload;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.context.FacesContext;

import org.primefaces.model.UploadedFile;

@ManagedBean(name="fileUploadModel1")
public class FileUploadModel {
	
	// fileupload için path 
	private final static String DESTINAION = "D:\\tmp\\";   

	private String tags; 

	private UploadedFile file;

	private String queryTags; 
	
	private List<FileObject> fileList;
	
	// index de submit butonuna bastýgýmýzda iþlemlerin yapýlacagý method
	public void upload() {    
		try {
			if (!validateFile()) {
				FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error! File[" + file.getFileName() + "] was already uploaded ","");
				FacesContext.getCurrentInstance().addMessage(null, msg);
				return;
			}
			
			copyFile(file.getFileName(), file.getInputstream());
			
			insertToDb();

			// file basarýlý olarak upload edilirse bu methoddan mesajý alýrýz
			giveMessage();  
			
			System.out.println(tags);
			tags = "";
		} catch (IOException | ClassNotFoundException | SQLException e) {
			FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error! ", e.getMessage());
			FacesContext.getCurrentInstance().addMessage(null, msg);
			e.printStackTrace();
		}
	}
	
	public void query() throws SQLException, ClassNotFoundException {
		String[] tags = queryTags.split(",");
		fileList = new ArrayList<FileObject>();
		Connection conn = DatabaseManager.getConnection();
		for (String tag : tags) {
			int id = findTagId(tag, conn);
			if (id != -1) {
				PreparedStatement stmt = conn.prepareStatement("SELECT * FROM file_tag WHERE tag_id = ?;");
				stmt.setInt(1, id);
				ResultSet rs = stmt.executeQuery();
				while(rs.next()) {
					int fileId = rs.getInt("file_id");
					
					stmt = conn.prepareStatement("SELECT file_name FROM file WHERE id = ?;");
					stmt.setInt(1, fileId);
					ResultSet rs2 = stmt.executeQuery(); 
					rs2.next();
					String fileName = rs2.getString("file_name");
					fileList.add(new FileObject(tag, fileName));
				}
			}
		}
		normalizeFileList();
	}
	private void normalizeFileList() {
		Map<String, String> map = new HashMap<String, String>();
		for (FileObject fileObject : fileList) {
			String key = fileObject.getFileName();
			if (map.containsKey(key)) { // map içersinde referans olarak verilen anahtar degeri true
				String value = map.get(key);   // referans olarak verilen anahtar degere karsýlýk gelen elemaný ver.
				value += ", " + fileObject.getTag();
				map.put(key, value);    // put ile tekrardan kacýnýrýz
			} else {
				map.put(key, fileObject.getTag());
			}
		}
		
		fileList = new ArrayList<FileObject>();
		for (String key : map.keySet()) {
			fileList.add(new FileObject(map.get(key), key));
		}
	}
	

	private boolean validateFile() throws ClassNotFoundException, SQLException {
		// is file already inserted?
		Connection conn = DatabaseManager.getConnection();
		PreparedStatement stmt = conn.prepareStatement("SELECT * FROM file WHERE file_name = ?;");
		stmt.setString(1, file.getFileName());
		ResultSet rs = stmt.executeQuery();
		return !rs.next();
	}

	private void giveMessage() {
		FacesMessage msg = new FacesMessage("Success! ", file.getFileName() + " is uploaded.");
		FacesContext.getCurrentInstance().addMessage(null, msg);
	}

	private void insertToDb() throws ClassNotFoundException, SQLException { //dataase ýnsert   
		int fileId = insertFileToDb(file.getFileName());     
		String[] parts = tags.split(",");   
		for (String tag : parts) {             // for dongusu içinde ýd buluyoruz    
			int tagId = insertTagToDb(tag);      
			insertFileTagToDb(fileId, tagId);
		}
	}

	private int insertFileToDb(String fileName) throws ClassNotFoundException, SQLException { 
		Connection conn = DatabaseManager.getConnection();   
		PreparedStatement stmt = conn.prepareStatement("INSERT INTO file (file_name) VALUES (?);");
		stmt.setString(1, fileName);
		stmt.executeUpdate();
		
		stmt = conn.prepareStatement("SELECT id FROM file WHERE file_name = ?;");
		stmt.setString(1, fileName);
		ResultSet rs = stmt.executeQuery(); //veritabanýndan donen verileri içinde tutar next ile veri satrýnýn ekler
		rs.next();
		
		return rs.getInt("id");
	}

	private int insertTagToDb(String tag) throws ClassNotFoundException, SQLException {
		Connection conn = DatabaseManager.getConnection();
		
		int id = findTagId(tag, conn);
		if (id != -1) {
			return id;
		}
		PreparedStatement stmt = conn.prepareStatement("INSERT INTO tag (tag_name) VALUES (?);");
		stmt.setString(1, tag);
		stmt.executeUpdate();
		
		stmt = conn.prepareStatement("SELECT id FROM tag WHERE tag_name = ?;");
		stmt.setString(1, tag);
		ResultSet rs = stmt.executeQuery();
		rs.next();
		
		return rs.getInt("id");
	}

	private int findTagId(String tag, Connection conn) throws SQLException {
		PreparedStatement stmt = conn.prepareStatement("SELECT id FROM tag WHERE tag_name = ?;");
		stmt.setString(1, tag);
		ResultSet rs = stmt.executeQuery();
		if (rs.next()) {
			return rs.getInt("id");
		}
		return -1;
	}

	//TODO: findfile by tag id fonksiyonu yazilacak
	
	private void insertFileTagToDb(int fileId, int tagId) throws ClassNotFoundException, SQLException {
		System.out.println("file id: " + fileId + " tag id: " + tagId);
		Connection conn = DatabaseManager.getConnection();
		PreparedStatement stmt = conn.prepareStatement("INSERT INTO file_tag (file_id, tag_id) VALUES (?, ?);");
		stmt.setInt(1, fileId);
		stmt.setInt(2, tagId);
		stmt.executeUpdate();
	}

	public void copyFile(String fileName, InputStream in) {
		try {
			// write the inputStream to a FileOutputStream
			File file = new File(DESTINAION + fileName); //dosya okuma
			OutputStream out = new FileOutputStream(file);  //dosya yazýrdmak

			int read = 0;
			byte[] bytes = new byte[1024];  //1024 boyutlu bir byte dizisi oluþturduk

			while ((read = in.read(bytes)) != -1) { // eðer okudumuz deðer -1 ise , dosyanýn sonu gelmiþ demektir.döngüyü kýrmamýz gerekir
				out.write(bytes, 0, read);
			}

			in.close();
			out.flush();
			out.close();

			System.out.println("New file created! " + file.getAbsolutePath()   //dosyanýn gerçek uzantýsýný belirtir
					+ " " + (file.length() / 1024) + " Kb");
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}

	public UploadedFile getFile() {
		return file;
	}

	public void setFile(UploadedFile file) {
		this.file = file;
	}

	public String getTags() {
		return tags;
	}

	public void setTags(String tags) {
		this.tags = tags;
	}

	public String getQueryTags() {
		return queryTags;
	}

	public void setQueryTags(String queryTags) {
		this.queryTags = queryTags;
	}

	public List<FileObject> getFileList() {
		return fileList;
	}

	public void setFileList(List<FileObject> fileList) {
		this.fileList = fileList;
	}

}