package cn.cerc.summer.android.basis.utils;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import cn.cerc.summer.android.basis.core.MySession;
import cn.cerc.summer.android.basis.forms.JavaScriptService;
import cn.cerc.summer.android.parts.barcode.FrmScanProduct;

/**
 * Created by Jason<sz9214e@qq.com> on 2017/8/9.
 */

public class ScanProduct implements JavaScriptService {
    @Override
    public String execute(Context context, JSONObject json) throws Exception {
        if (!json.has("title")) {
            return "title is null.";
        }
        if (!json.has("homeUrl")) {
            return "homeUrl is null.";
        }
        if (!json.has("returnUrl")) {
            return "returnUrl is null.";
        }
        if (!json.has("appendUrl")) {
            return "appendUrl is null.";
        }
        if (!json.has("modifyUrl")) {
            return "modifyUrl is null.";
        }
        if (!json.has("deleteUrl")) {
            return "deleteUrl is null.";
        }
        if (!json.has("viewUrl")) {
            return "viewUrl is null.";
        }

        String token = null;
        if (json.has("token")) {
            token = json.getString("token");
        } else {
            if (MySession.getInstance().getToken() != null) {
                token = MySession.getInstance().getToken();
            }
        }
        if (token == null || "".equals(token)) {
            return "token is null.";
        }

        Log.d("ScanProduct", json.getString("title"));
        MySession.getInstance().setToken(token);
        FrmScanProduct.startForm(context, json.getString("title"), json.getString("homeUrl"),
                json.getString("returnUrl"), json.getString("appendUrl"), json.getString("modifyUrl"),
                json.getString("deleteUrl"), json.getString("viewUrl"));
        return "true";
    }
}
