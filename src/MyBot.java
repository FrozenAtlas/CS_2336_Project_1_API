// axd171430 Khan Semester Project 1, API Chatbot
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jibble.pircbot.*;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;


public class MyBot extends PircBot{
	
	public MyBot() {
		this.setName("MyBot-RI"); // set bot name and password to identify
		this.identify("password");
	}
	
	public void onMessage(String channel, String sender, String login, String hostname, String message) {
		if (message.equalsIgnoreCase("time")) { // time command, outputs current time
			String time = new java.util.Date().toString();
			sendMessage(channel, sender + ": The time is now " + time);
		}
		else if (message.equalsIgnoreCase("disconnect")) { // disconnects from server
			sendMessage(channel, "Disconnecting");
			disconnect();
		}
		// weather command, input ZIP, output current temp, high, low
		else if (message.toLowerCase().indexOf("weather")!=-1) { 
			if(!message.matches(".*\\d+.*")) // make sure message contains numbers
				sendMessage(channel, "No ZIP code provided.");
			else {
				String ZIP = getNumbers(message);
				if(ZIP == null) { // make sure they are 5 digit valid zips
		        	sendMessage(channel, "Not a valid ZIP code.");
		        }
				else {
					double[] temps = kelvinToFahrenheit(getWeather(ZIP)); // get the weather with API, and convert it to fahrenheit
					if(temps[3] == 404.0) { // if there is no file then the zip is not valid
						sendMessage(channel, "Not a valid ZIP code.");		
					}
					else { // output temps
						sendMessage(channel,sender+": The weather in " + ZIP +" is going to be "+temps[0]+"F with a low of "+temps[1]+"F and a high of "+temps[2]+"F.");
					}
				}
			}
		}
		// translate to language command
		else if (message.toLowerCase().indexOf("translate to ")!=-1) {
			String input = message;
			String regex = "\\s*\\btranslate to \\b\\s*";
			input = input.replaceAll(regex, "");
			String [] inputArr = input.split(" ", 2);
			//inputArr[0] // language
			//inputArr[1] // rest of string
			sendMessage(channel, "Original message: " + inputArr[1]);
			try {
				if(inputArr[0].length() > 2) {
					String locale = getLocale(inputArr[0]);
					if(locale == null) {
						sendMessage(channel, "No language to translate to provided. ");
					}
					else{
						sendMessage(channel, "Translated message: " + translateto(locale,inputArr[1]));
					}
				}
				else {
					sendMessage(channel, "Translated message: " + translateto(inputArr[0],inputArr[1]));
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		// translate to english command
		else if (message.toLowerCase().indexOf("translate")!=-1) {
			String input = message;
			String regex = "\\s*\\btranslate\\b\\s*";
			input = input.replaceAll(regex, "");
			sendMessage(channel, "Original message: " + input);
			try {
				sendMessage(channel, "Translated message: " + translate(input));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// help command
		else if (message.equalsIgnoreCase("help")) {
			sendMessage(channel, "Available Commands:");
			sendMessage(channel, "\ttime : return current time");
			sendMessage(channel, "\tweather [zip] : returns the weather in zip code (if \"weather\" and a valid ZIP is found in a message it will still work)");
			sendMessage(channel, "\ttranslate [message] : returns message translated to english");
			sendMessage(channel, "\ttranslate to [language] [message] : return message translated to requested language");
			sendMessage(channel, "\tdisconnect : disconnects bot from current server and channel");
		}
	}
	
	// get numbers from string, aka get the ZIP from the message
	private static final Pattern p = Pattern.compile("[0-9]{5}");
	public String getNumbers(String message) {
		
		String zipcode = "";
        Matcher m_zipcode = p.matcher(message);

        if (m_zipcode.find()) {
            zipcode = m_zipcode.group(0);
        }
        else {
            return null;
        }
        return zipcode;
	}
	// method to get the weather from the weather api
	public double[] getWeather(String zIP) {
		double[] temps = {0,0,0,0};
		String response = null;
		String apiKey = "331ec7fc6a5fcbebf96c9907a0182d97";
		try {
			// set URL
			URLConnection con = new URL("http://api.openweathermap.org/data/2.5/weather?q=" + zIP + ",us&APPID=" + apiKey).openConnection();
			// try accessing website for json then read it in with scanner
			try(Scanner scanner = new Scanner(con.getInputStream());){
			    response = scanner.useDelimiter("\\A").next();
			    
				temps = parseWeather(response);				
			} catch (FileNotFoundException e) {
				// will produce null pointer so we will manually set the 404
				temps[3] = 404.0; 
			}
			
			((HttpURLConnection) con).disconnect();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return temps;
	}
	// method to parse json given by getWeather
	// outputs current temp, high temp, low temp, and http response code to check if file does not exist
	public double[] parseWeather(String JSONinput) {
		double[] temps = {0,0,0,0};
		JsonElement jelement = new JsonParser().parse(JSONinput); //parse string into a json element with gson
		JsonObject  jobject = jelement.getAsJsonObject(); // make json element into json object
		String response = jobject.get("cod").toString(); // grabbing response code
		jobject = jobject.getAsJsonObject("main"); // get main array as json object to file through it
		if(!(response == "404")) {	// if the file does exist 	
			String currTemp = jobject.get("temp").toString(); // pull temps
			String minTemp = jobject.get("temp_min").toString();
			String maxTemp = jobject.get("temp_max").toString();
			temps[0] = Double.parseDouble(currTemp); // assign temps in array
			temps[1] = Double.parseDouble(minTemp);
			temps[2] = Double.parseDouble(maxTemp);
		}
		temps[3] = Double.parseDouble(response); // always pull http response code to check for 404's
		return temps;
	}
	// method that converts Kelvin units to Fahrenheit units
	public double[] kelvinToFahrenheit(double[] kTemps) {
		double[] fTemps = {0,0,0,kTemps[3]};
		for(int i =0; i<kTemps.length-1; i++) {
			fTemps[i] = ((int)(((kTemps[i] - 273.15) * 9/5 +32)*10))/10.0;
		}
		return fTemps;	
	}
	public String translateto(String language, String input) {
		Translate translate = TranslateOptions.getDefaultInstance().getService();
		Translation translation = 
				translate.translate(input,
						Translate.TranslateOption.targetLanguage(language));
		
		return translation.getTranslatedText();
	}
	public String translate(String input) throws Exception{
		Translate translate = TranslateOptions.getDefaultInstance().getService();
		Translation translation = 
				translate.translate(input,
						Translate.TranslateOption.targetLanguage("en"));
		
		return translation.getTranslatedText();
	}

	public String getLocale(String string) {
		String langArray[] = {"afrikaans","albanian","arabic","azerbaijani","basque","bengali",
				"belarusian","bulgarian","catalan","chinese simplified","chinese traditional",
				"croatian","czech","danish","dutch","english","esperanto","estonian","filipino",
				"finnish","french","galician","georgian","german","greek","gujarati",
				"haitian creole","hebrew","hindi","hungarian","icelandic","indonesian","irish",
				"italian","japanese","kannada","korean","latin","latvian","lithuanian","macedonian",
				"malay","maltese","norwegian","persian","polish","portuguese","romanian","russian",
				"serbian","slovak","slovenian","spanish","swahili","swedish","tamil","telugu",
				"thai","turkish","ukrainian","urdu","vietnamese","welsh","yiddish"};
		String localeArray[] = {"af","sq","ar","az","eu","bn","be","bg","ca","zh-CN","zh-TW",
				"hr","cs","da","nl","en","eo","et","tl","fi","fr","gl","ka","de","el","gu",
				"ht","iw","hi","hu","is","Id","ga","it","ja","kn","ko","la","lv","lt","mk",
				"ms","mt","no","fa","pl","pt","ro","ru","sr","sk","sl","es","sw","sv","ta",
				"te","th","tr","uk","ur","vi","cy","yi"};
		for(int i =0; i < langArray.length; i++) {
			if(langArray[i].equals(string)) { // if the language is supported then it will output the locale code
				return localeArray[i];
			}
		}
		
		return null;
	}
}
