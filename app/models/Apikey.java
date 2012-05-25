package models;

import javax.persistence.Entity;

import play.db.jpa.Model;

@Entity(name = "apikey")
public class Apikey extends Model {
	public String apikey;
	public String appname;
	public String userid;
}
