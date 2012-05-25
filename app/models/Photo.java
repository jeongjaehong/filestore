package models;

import java.util.Date;

import javax.persistence.Entity;

import play.db.jpa.Model;

@Entity(name = "photo")
public class Photo extends Model {
	public String photoid;
	public String filename;
	public long filesize;
	public String secret;
	public String title;
	public String description;
	// upload info
	public String apikey;
	public Date uploaddate;
	// download info
	public long downloadcnt;
	public Date lastdownloaddate;
}
