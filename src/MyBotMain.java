
public class MyBotMain {
	
	public static void main(String[] args) throws Exception{		
		// start bot
		MyBot bot = new MyBot();
		
		// Enable debugging output
		bot.setVerbose(true);
		
		// connect to IRC server
		bot.connect("irc.freenode.net");
		
		// join #pircbot
		bot.joinChannel("#pircbot");
		
	}
}
