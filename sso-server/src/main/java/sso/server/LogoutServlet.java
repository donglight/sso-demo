package sso.server;

import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;


/**
 * 单点注销servlet
 * @author donglight
 */
@WebServlet(name = "LogoutServlet", urlPatterns = "/logout")
public class LogoutServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        //把当前用户与SSO关联的全局session对象中的token取出来
        String token = (String) request.getSession().getAttribute("token");
        //returnUrl代表是在哪个系统来SSO注销
        String returnUrl = request.getParameter("returnUrl");
        Map<String, Map<String, String>> tokenMap = TokenMap.getTokenMap();
        //addressMap为在SSO注册过的系统的returnUrl
        Map<String, String> addressMap = null;
        if (token != null) {
            addressMap = tokenMap.remove(token);
        }
        // 获得Http客户端(可以理解为:你得先有一个浏览器;注意:实际上HttpClient与浏览器是不一样的)
        CloseableHttpClient httpClient = null;
        HttpPost httpPost;
        try {
            if (addressMap != null) {
                httpClient = HttpClientBuilder.create().build();
                //使用httpclient通知登录过的系统消除它们自己的局部会话
                for (Map.Entry<String, String> entry : addressMap.entrySet()) {
                    //key为注册的地址(returnUrl),value为jsessionid

                    httpPost = new HttpPost(entry.getKey() + "logout?");
                    //带上cookie:jsessionid去登录过的系统消除局部会话，不然会是httpclient产生的新session
                    // 不是原来浏览器的jsesionid对应的session对象，造成子系统注销不了
                    httpPost.setHeader("Cookie", "JSESSIONID=" + entry.getValue());
                    httpClient.execute(httpPost);
                }

            }
        } catch (ClientProtocolException e) {
            System.err.println("Http协议出现问题");
            e.printStackTrace();
        } catch (ParseException e) {
            System.err.println("解析错误");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("IO异常");
            e.printStackTrace();
        } finally {
            if (httpClient != null) {
                httpClient.close();
            }
        }
        //清除全局会话
        request.getSession().invalidate();
        //跳转到登录界面
        response.sendRedirect("http://sso-server:8081/login.jsp?returnUrl="+returnUrl);
    }


    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }
}
