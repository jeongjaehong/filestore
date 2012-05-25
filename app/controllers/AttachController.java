package controllers;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import models.Attach;
import play.Play;
import play.data.validation.Required;
import play.libs.Files;
import play.mvc.Controller;

/**
 * 첨부파일 업로드 / 다운로드 컨트롤러
 */
public class AttachController extends Controller {

	/**
	 * 첨부파일 업로드 폼
	 */
	public static void uploadForm() {
		render();
	}

	/**
	 * 첨부파일 삭제 폼
	 */
	public static void deleteForm() {
		render();
	}

	/**
	 * 첨부파일 목록을 Json 형식으로 리턴한다.
	 * 
	 * @param apikey
	 * @param perpage
	 * @param page
	 */
	public static void listJson(@Required String apikey, Integer perpage, Integer page) {
		try {
			int l_perpage = (perpage == null) ? 100 : perpage;
			int l_page = (page == null) ? 1 : page;
			List<Attach> l_attachList = Attach.find("apikey = ? order by id desc", apikey).from((l_page - 1) * l_perpage).fetch(l_perpage);
			renderJSON(l_attachList);
		} catch (Exception e) {
			e.printStackTrace();
			notFound();
		}
	}

	/**
	 * 첨부파일 목록을 Xml 형식으로 리턴한다.
	 * 
	 * @param apikey
	 * @param perpage
	 * @param page
	 */
	public static void listXml(@Required String apikey, Integer perpage, Integer page) {
		try {
			int l_perpage = (perpage == null) ? 100 : perpage;
			int l_page = (page == null) ? 1 : page;
			List<Attach> l_attachList = Attach.find("apikey = ? order by id desc", apikey).from((l_page - 1) * l_perpage).fetch(l_perpage);
			renderXml(l_attachList);
		} catch (Exception e) {
			e.printStackTrace();
			notFound();
		}
	}

	/**
	 * 첨부파일을 업로드 한다.
	 * 
	 * @param apikey
	 * @param title
	 * @param description
	 * @param myfile
	 */
	public static void upload(@Required String apikey, String title, String description, @Required File myfile) {
		Map<String, Object> l_result = new HashMap<String, Object>();
		try {
			Date l_uploaddate = new Date();
			Calendar l_calendar = Calendar.getInstance();
			l_calendar.setTime(l_uploaddate);
			String l_attachid = UUID.randomUUID().toString().replaceAll("-", "");
			String l_secret = UUID.randomUUID().toString().replaceAll("-", "");
			String l_datafolder = Play.configuration.getProperty("attach.datafolder");
			String l_datepath = l_calendar.get(Calendar.YEAR) + File.separator + (l_calendar.get(Calendar.MONTH) + 1) + File.separator + l_calendar.get(Calendar.DATE);
			File l_absolutepath = new File(l_datafolder + File.separator + apikey + File.separator + l_datepath + File.separator + l_attachid);
			if (!l_absolutepath.exists()) {
				l_absolutepath.mkdirs();
			}
			File l_newFile = new File(l_absolutepath, myfile.getName());
			myfile.renameTo(l_newFile);

			Attach l_attach = new Attach();
			l_attach.attachid = l_attachid;
			l_attach.filename = l_newFile.getName();
			l_attach.filesize = l_newFile.length();
			l_attach.secret = l_secret;
			l_attach.apikey = apikey;
			l_attach.title = title;
			l_attach.description = description;
			l_attach.uploaddate = new Date();
			l_attach.downloadcnt = 0;
			l_attach.save();

			l_result.put("ok", true);
			l_result.put("message", "success");
			l_result.put("attachid", l_attachid);
			l_result.put("size", l_newFile.length());
			l_result.put("secret", l_secret);
		} catch (Exception e) {
			e.printStackTrace();
			l_result.put("ok", false);
			l_result.put("message", e.getMessage());
		} finally {
			renderJSON(l_result);
		}
	}

	/**
	 * 첨부파일을 다운로드 한다.
	 * 
	 * @param attachid
	 */
	public static void download(@Required String attachid) {
		try {
			Attach l_attach = Attach.find("byAttachid", attachid).first();
			String l_datafolder = Play.configuration.getProperty("attach.datafolder");
			Calendar l_calendar = Calendar.getInstance();
			l_calendar.setTime(l_attach.uploaddate);
			String l_datepath = l_calendar.get(Calendar.YEAR) + File.separator + (l_calendar.get(Calendar.MONTH) + 1) + File.separator + l_calendar.get(Calendar.DATE);
			String l_absolutepath = l_datafolder + File.separator + l_attach.apikey + File.separator + l_datepath + File.separator + l_attach.attachid;
			File l_myfile = new File(l_absolutepath, l_attach.filename);
			if (l_myfile.exists()) {
				l_attach.downloadcnt++;
				l_attach.lastdownloaddate = new Date();
				l_attach.save();
				renderBinary(l_myfile);
			} else {
				notFound();
			}
		} catch (Exception e) {
			e.printStackTrace();
			notFound();
		}
	}

	/**
	 * 첨부파일을 삭제한다.
	 * 
	 * @param apikey
	 * @param attachid
	 * @param secret
	 */
	public static void delete(@Required String apikey, @Required String attachid, @Required String secret) {
		Map<String, Object> l_result = new HashMap<String, Object>();
		try {
			Attach l_attach = Attach.find("byAttachid", attachid).first();
			if (l_attach != null) {
				if (l_attach.apikey.equals(apikey) && l_attach.secret.equals(secret)) {
					String l_datafolder = Play.configuration.getProperty("attach.datafolder");
					Calendar l_calendar = Calendar.getInstance();
					l_calendar.setTime(l_attach.uploaddate);
					String l_datepath = l_calendar.get(Calendar.YEAR) + File.separator + (l_calendar.get(Calendar.MONTH) + 1) + File.separator + l_calendar.get(Calendar.DATE);
					File l_absolutepath = new File(l_datafolder + File.separator + l_attach.apikey + File.separator + l_datepath + File.separator + l_attach.attachid);
					if (l_absolutepath.exists()) {
						Files.deleteDirectory(l_absolutepath);
						l_attach.delete();
						l_result.put("ok", true);
						l_result.put("message", "success");
					} else {
						l_result.put("ok", false);
						l_result.put("message", "file not found");
					}
				} else {
					l_result.put("ok", false);
					l_result.put("message", "apikey, secret error");
				}
			} else {
				l_result.put("ok", false);
				l_result.put("message", "db object not found");
			}
		} catch (Exception e) {
			e.printStackTrace();
			l_result.put("ok", false);
			l_result.put("message", e.getMessage());
		} finally {
			renderJSON(l_result);
		}
	}
}
