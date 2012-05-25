package controllers;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import models.Photo;
import play.Play;
import play.data.validation.Required;
import play.libs.Files;
import play.libs.Images;
import play.mvc.Controller;

/**
 * 사진 업로드 / 다운로드 컨트롤러
 */
public class PhotoController extends Controller {

	/**
	 * 사진 업로드 폼
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
	 * 사진 목록을 Json 형식으로 리턴한다.
	 * 
	 * @param apikey
	 * @param perpage
	 * @param page
	 */
	public static void listJson(@Required String apikey, Integer perpage, Integer page) {
		try {
			int l_perpage = (perpage == null) ? 100 : perpage;
			int l_page = (page == null) ? 1 : page;
			List<Photo> l_photoList = Photo.find("apikey = ? order by id desc", apikey).from((l_page - 1) * l_perpage).fetch(l_perpage);
			renderJSON(l_photoList);
		} catch (Exception e) {
			e.printStackTrace();
			notFound();
		}
	}

	/**
	 * 사진 목록을 Xml 형식으로 리턴한다.
	 * 
	 * @param apikey
	 * @param perpage
	 * @param page
	 */
	public static void listXml(@Required String apikey, Integer perpage, Integer page) {
		try {
			int l_perpage = (perpage == null) ? 100 : perpage;
			int l_page = (page == null) ? 1 : page;
			List<Photo> l_photoList = Photo.find("apikey = ? order by id desc", apikey).from((l_page - 1) * l_perpage).fetch(l_perpage);
			renderXml(l_photoList);
		} catch (Exception e) {
			e.printStackTrace();
			notFound();
		}
	}

	/**
	 * 사진을 업로드 한다.
	 * 
	 * @param apikey
	 * @param title
	 * @param description
	 * @param myphoto
	 */
	public static void upload(@Required String apikey, String title, String description, @Required File myphoto) {
		Map<String, Object> l_result = new HashMap<String, Object>();
		try {
			Date l_uploaddate = new Date();
			Calendar l_calendar = Calendar.getInstance();
			l_calendar.setTime(l_uploaddate);
			String l_photoid = UUID.randomUUID().toString().replaceAll("-", "");
			String l_secret = UUID.randomUUID().toString().replaceAll("-", "");
			String l_datafolder = Play.configuration.getProperty("photo.datafolder");
			String l_datepath = l_calendar.get(Calendar.YEAR) + File.separator + (l_calendar.get(Calendar.MONTH) + 1) + File.separator + l_calendar.get(Calendar.DATE);
			File l_absolutepath = new File(l_datafolder + File.separator + apikey + File.separator + l_datepath + File.separator + l_photoid);
			if (!l_absolutepath.exists()) {
				l_absolutepath.mkdirs();
			}
			File l_newFile = new File(l_absolutepath, myphoto.getName());
			myphoto.renameTo(l_newFile);
			String[] l_sizeSuffixes = { "s", "q", "t", "m", "n", "-", "z", "c", "b" };
			int index = l_newFile.getName().lastIndexOf(".");
			String l_fileName = l_newFile.getName().substring(0, index);
			String l_fileExt = l_newFile.getName().substring(index + 1);
			for (String l_suffix : l_sizeSuffixes) {
				File to = new File(l_newFile.getParent(), l_fileName + "_" + l_suffix + "." + l_fileExt);
				resizeImage(l_newFile, to, l_suffix);
			}
			// DB저장
			Photo l_photo = new Photo();
			l_photo.photoid = l_photoid;
			l_photo.filename = l_newFile.getName();
			l_photo.filesize = l_newFile.length();
			l_photo.secret = l_secret;
			l_photo.apikey = apikey;
			l_photo.title = title;
			l_photo.description = description;
			l_photo.uploaddate = l_uploaddate;
			l_photo.downloadcnt = 0;
			l_photo.save();

			l_result.put("ok", true);
			l_result.put("message", "success");
			l_result.put("photoid", l_photoid);
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
	 * 사진을 다운로드 한다.
	 * 
	 * @param photoid_size
	 */
	public static void download(@Required String photoid_size) {
		try {
			String[] l_arr = photoid_size.split("_");
			Photo l_photo = Photo.find("byPhotoid", l_arr[0]).first();
			String l_datafolder = Play.configuration.getProperty("photo.datafolder");
			Calendar l_calendar = Calendar.getInstance();
			l_calendar.setTime(l_photo.uploaddate);
			String l_datepath = l_calendar.get(Calendar.YEAR) + File.separator + (l_calendar.get(Calendar.MONTH) + 1) + File.separator + l_calendar.get(Calendar.DATE);
			String l_absolutepath = l_datafolder + File.separator + l_photo.apikey + File.separator + l_datepath + File.separator + l_photo.photoid;
			File l_myfile = null;
			if (l_arr.length > 1) {
				String l_suffix = l_arr[1];
				int index = l_photo.filename.lastIndexOf(".");
				String l_fileName = l_photo.filename.substring(0, index);
				String l_fileExt = l_photo.filename.substring(index + 1);
				l_myfile = new File(l_absolutepath, l_fileName + "_" + l_suffix + "." + l_fileExt);
			} else {
				l_myfile = new File(l_absolutepath, l_photo.filename);
			}
			if (l_myfile.exists()) {
				l_photo.downloadcnt++;
				l_photo.lastdownloaddate = new Date();
				l_photo.save();
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
	 * 사진을 삭제한다.
	 * 
	 * @param apikey
	 * @param photoid
	 * @param secret
	 */
	public static void delete(@Required String apikey, @Required String photoid, @Required String secret) {
		Map<String, Object> l_result = new HashMap<String, Object>();
		try {
			Photo l_photo = Photo.find("byPhotoid", photoid).first();
			if (l_photo != null) {
				if (l_photo.apikey.equals(apikey) && l_photo.secret.equals(secret)) {
					String l_datafolder = Play.configuration.getProperty("photo.datafolder");
					Calendar l_calendar = Calendar.getInstance();
					l_calendar.setTime(l_photo.uploaddate);
					String l_datepath = l_calendar.get(Calendar.YEAR) + File.separator + (l_calendar.get(Calendar.MONTH) + 1) + File.separator + l_calendar.get(Calendar.DATE);
					File l_absolutepath = new File(l_datafolder + File.separator + l_photo.apikey + File.separator + l_datepath + File.separator + l_photo.photoid);
					if (l_absolutepath.exists()) {
						Files.deleteDirectory(l_absolutepath);
						l_photo.delete();
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
				l_result.put("message", "apikey, secret error");
			}
		} catch (Exception e) {
			e.printStackTrace();
			l_result.put("ok", false);
			l_result.put("message", e.getMessage());
		} finally {
			renderJSON(l_result);
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////////Private 메소드

	/**
	 * 원본파일을 접미사에 따라 해당 크기로 리사이즈한다.
	 * 
	 * @param originImage
	 * @param to
	 * @param suffix
	 */
	private static void resizeImage(File originImage, File to, String suffix) {
		int l_size = -1;
		if ("s".equals(suffix)) {
			l_size = 75;
		} else if ("q".equals(suffix)) {
			l_size = 150;
		} else if ("t".equals(suffix)) {
			l_size = 100;
		} else if ("m".equals(suffix)) {
			l_size = 240;
		} else if ("n".equals(suffix)) {
			l_size = 320;
		} else if ("-".equals(suffix)) {
			l_size = 500;
		} else if ("z".equals(suffix)) {
			l_size = 640;
		} else if ("c".equals(suffix)) {
			l_size = 800;
		} else if ("b".equals(suffix)) {
			l_size = 1024;
		}
		Images.resize(originImage, to, l_size, l_size, true);
	}
}
