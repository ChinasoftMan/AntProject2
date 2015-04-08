package com.gnb;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;


/**
 * java语言Windows平台下的构建实现类
 * 
 * @author guoningbo
 *
 */
public class JavaForWinBuildService {

	/* 
	 * 创建构建任务
	 * (non-Javadoc)
	 * @see com.chinasoft.service.IBuildService#createJob(com.chinasoft.entities.Build)
	 */
	public String createJob(String ip,String port,String jobName,String repository) throws ClientProtocolException,
			IOException, DocumentException {
		String result = "";

		CloseableHttpClient httpclient = HttpClients.createDefault();
		CloseableHttpResponse response;
		try {
			//拼装创建构建任务名称接口
			String createURL = "http://" + ip + ":" + port
					+ "/jenkins/view/All/createItem";

			HttpPost httppost = new HttpPost(createURL);
			/*
			 * 封装构建任务名称接口所需参数
			 */
			List<NameValuePair> formparams = new ArrayList<NameValuePair>();
			formparams.add(new BasicNameValuePair("name", jobName));
			formparams.add(new BasicNameValuePair("mode",
					"hudson.model.FreeStyleProject"));
			formparams.add(new BasicNameValuePair("from", ""));
			UrlEncodedFormEntity uefEntity;
			uefEntity = new UrlEncodedFormEntity(formparams, "UTF-8");
			httppost.setEntity(uefEntity);
			response = httpclient.execute(httppost);
			/*
			 * 根据返回状态码判断是否成功,成功后调用配置构建任务方案方法
			 */
			try {
				if (response.getStatusLine().getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY) {
					result = configJob(ip,port,jobName,repository);
				}
			} finally {
				response.close();
			}
			
		} finally {
			httpclient.close();
		}
	
		return result;
	}

	/* 
	 * 配置构建任务
	 * (non-Javadoc)
	 * @see com.chinasoft.service.IBuildService#configJob(com.chinasoft.entities.Build)
	 */
	public String configJob(String ip,String port,String jobName,String repository) throws IOException, DocumentException {
		String result = "";
		/*
		 * 从构建配置文件build.properties获取jenkins的工作目录和配置方案模板工作目录
		 
		InputStream inputStream = this.getClass().getResourceAsStream("build.properties");
		Properties buildProperties = new Properties();
		buildProperties.load(inputStream);
		String jenkinsHome = (String)buildProperties.get("Jenkins_Home");
		String jobHome = jenkinsHome+"/jobs/"+ jobName;
		String configHome = jobHome+"/config.xml";
		String templateConfigHome = (String)buildProperties.get("Template_Config_Home");
        */
		String jobHome = "E:/jenkins_work/jobs/"+ jobName;
		String configHome = jobHome+"/config.xml";
		String templateConfigHome = "C:/Build_Template/config.xml";
		/*
		 * 修改构建任务模板文件的库地址并将配置方案写到对应的构建任务目录下
		 */
		Document document=new SAXReader().read(templateConfigHome);
		if (document == null) {
			System.out.println(templateConfigHome+"is not exist");
		} else {
			Node node = document.selectSingleNode("//project/scm/userRemoteConfigs/hudson.plugins.git.UserRemoteConfig/url");
			node.setText(repository);
			XMLWriter writer = new XMLWriter(new FileWriter(new File(configHome)));  
			writer.write(document);  
			writer.close();
			//重新加载配置的构建任务方案
			result = reloadConfig(ip,port);
		}
		return result;
	}
	
	/**
	 * 重新加载配置的构建任务方案
	 * @param build
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String reloadConfig(String ip,String port) throws ClientProtocolException, IOException{
		String result = "";
		//拼装重新加载构建任务名称接口
		String reloadURL = "http://" + ip + ":" + port
				+ "/jenkins/reload";
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpPost httppost = new HttpPost(reloadURL);
		CloseableHttpResponse response = httpclient.execute(httppost);
		try{
			//调用成功后返回结果设置为1
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY) {
				result ="1";
			}
		} finally {
			response.close();
			httpclient.close();
		}
		
		return result;
	}

	/* 执行构建任务
	 * (non-Javadoc)
	 * @see com.chinasoft.service.IBuildService#buildJob(com.chinasoft.entities.Build)
	 */
	public String buildJob(String ip,String port,String jobName,String operation,String delay) throws ClientProtocolException, IOException {
		String result = "";
        //拼装build的URL
		String buildURL = "http://" + ip + ":" + port + "/jenkins/job/"
				+ jobName + "/" + operation;
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpPost httppost = new HttpPost(buildURL);
		if (null != delay) {
			List<NameValuePair> formparams = new ArrayList<NameValuePair>();
			formparams.add(new BasicNameValuePair("delay", delay + "sec"));
			UrlEncodedFormEntity uefEntity;
			uefEntity = new UrlEncodedFormEntity(formparams, "UTF-8");
			httppost.setEntity(uefEntity);
		}
		CloseableHttpResponse response = httpclient.execute(httppost);
		try {
			//调用成功后返回结果设置为1
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY) {
				result ="1";
			}
		} finally {
			response.close();
			httpclient.close();
		}
		
		return result;
	}
	
	public static void main(String[] args) throws ClientProtocolException, IOException, DocumentException{
		
		JavaForWinBuildService service = new JavaForWinBuildService();
		//service.createJob("172.16.3.160","8080","104","https://github.com/ChinasoftMan/AntProject.git");
		service.buildJob("172.16.3.160","8080","104","build","0");
		
	}

}
