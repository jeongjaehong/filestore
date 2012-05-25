package models;

import java.util.Date;

import javax.persistence.Entity;

import play.db.jpa.Model;

@Entity(name = "attach")
public class Attach extends Model {
	public String attachid;
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
