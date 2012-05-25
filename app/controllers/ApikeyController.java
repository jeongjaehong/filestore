package controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import models.Apikey;
import models.Attach;
import play.data.validation.Required;
import play.mvc.Controller;

public class ApikeyController extends Controller {

	public static void genkeyForm() {
		render();
	}

	public static void listJson(Integer perpage, Integer page) {
		try {
			int l_perpage = (perpage == null) ? 100 : perpage;
			int l_page = (page == null) ? 1 : page;
			List<Apikey> l_apikeyList = Apikey.find("order by id desc").from((l_page - 1) * l_perpage).fetch(l_perpage);
			renderJSON(l_apikeyList);
		} catch (Exception e) {
			e.printStackTrace();
			notFound();
		}
	}

	public static void listXml(Integer perpage, Integer page) {
		try {
			int l_perpage = (perpage == null) ? 100 : perpage;
			int l_page = (page == null) ? 1 : page;
			List<Apikey> l_apikeyList = Apikey.find("order by id desc").from((l_page - 1) * l_perpage).fetch(l_perpage);
			renderXml(l_apikeyList);
		} catch (Exception e) {
			e.printStackTrace();
			notFound();
		}
	}

	public static void genkey(String appname, String userid) {
		Map<String, Object> l_result = new HashMap<String, Object>();
		try {
			String l_uuid = UUID.randomUUID().toString().replaceAll("-", "");
			Apikey l_apikey = new Apikey();
			l_apikey.apikey = l_uuid;
			l_apikey.appname = appname;
			l_apikey.userid = userid;
			l_apikey.save();
			// 결과리턴
			l_result.put("ok", true);
			l_result.put("apikey", l_uuid);
		} catch (Exception e) {
			e.printStackTrace();
			l_result.put("ok", false);
		} finally {
			renderJSON(l_result);
		}
	}
}
