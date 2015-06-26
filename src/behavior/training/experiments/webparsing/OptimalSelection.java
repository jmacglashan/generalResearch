package behavior.training.experiments.webparsing;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.auxiliary.common.StateYAMLParser;
import burlap.oomdp.core.Domain;
import domain.singleagent.sokoban2.Sokoban2Domain;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author James MacGlashan.
 */
public class OptimalSelection {


	public List<String> userDirectoryPaths(String directoryPath){

		if(!directoryPath.endsWith("/")){
			directoryPath = directoryPath + "/";
		}

		File dir = new File(directoryPath);

		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				if(name.startsWith("null")){
					return false;
				}
				return true;
			}
		};
		String[] children = dir.list(filter);

		List<String> userDirPaths = new ArrayList<String>(children.length);


		for(int i = 0; i < children.length; i++){
			String userDir = directoryPath + children[i];
			userDirPaths.add(userDir);
		}

		return userDirPaths;

	}




	public List <CommandEpisode> getCommandEpisodes(String userSessionPath, Domain d, StateParser sp){

		if(!userSessionPath.endsWith("/")){
			userSessionPath = userSessionPath + "/";
		}

		File dir = new File(userSessionPath);
		final String ext = ".episode";

		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				if(name.endsWith(ext)){
					return true;
				}
				return false;
			}
		};
		String[] children = dir.list(filter);

		List<CommandEpisode> trials = new ArrayList<CommandEpisode>(children.length);

		for(int i = 0; i < children.length; i++){
			String episodeFile = userSessionPath + children[i];
			String commandFile = userSessionPath + episodeFileToCommandFile(children[i]);
			EpisodeAnalysis ea = EpisodeAnalysis.parseFileIntoEA(episodeFile, d, sp);
			String command = getCommand(commandFile);
			trials.add(new CommandEpisode(this.trialId(children[i]), command, ea));
		}


		return trials;
	}


	public List <UserData> collectUserData(List<String> userTestPaths, Domain d, StateParser sp){

		List <UserData> results = new ArrayList<UserData>(userTestPaths.size());

		for(String userPath : userTestPaths){
			UserData ud = new UserData(userPath);
			List <CommandEpisode> trials = this.getCommandEpisodes(userPath, d, sp);
			ud.trials = trials;
			results.add(ud);
		}


		return results;
	}



	public String episodeFileToCommandFile(String episodeFile){
		String noExt = episodeFile.substring(0, episodeFile.indexOf('.'));
		String commandName = noExt.replaceAll("episode", "commands");
		String fullName = commandName + ".commands";
		return fullName;
	}

	public String trialId(String episodeFile){
		String noExt = episodeFile.substring(0, episodeFile.indexOf('.'));
		String id = noExt.replaceAll("episode", "");
		return id;
	}


	public String getCommand(String commandFilePath){
		String res = null;
		try {
			BufferedReader in = new BufferedReader(new FileReader(commandFilePath));
			res = in.readLine().trim();
		} catch(FileNotFoundException e) {
			e.printStackTrace();
		} catch(IOException e) {
			e.printStackTrace();
		}
		return res;
	}


	public static class UserData{
		public String userSession;
		public List <CommandEpisode> trials = new ArrayList<CommandEpisode>();

		public UserData(String userSession){
			this.userSession = userSession;
		}

	}


	public static class CommandEpisode{
		public String trialId;
		public String command;
		public EpisodeAnalysis episode;

		public CommandEpisode(String trialId, String command, EpisodeAnalysis episode){
			this.trialId = trialId;
			this.command = command;
			this.episode = episode;
		}

	}

	public static void writeIsOptimal(String userPath, String trialId, int [] isOptimal){

		String isOptimalName = "optimal" + trialId + ".opt";
		String fullPath = userPath + "/" + isOptimalName;

		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(fullPath));
		} catch(IOException e) {
			e.printStackTrace();
		}

		for(int o : isOptimal){
			try {
				out.write(o + "\n");
			} catch(IOException e) {
				e.printStackTrace();
			}
		}

		try {
			out.close();
		} catch(IOException e) {
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {

		String path = "oomdpResearch/amt_soko_logs_3";

		Sokoban2Domain soko = new Sokoban2Domain();
		soko.includeDirectionAttribute(true);
		soko.includePullAction(true);
		Domain d = soko.generateDomain();
		StateParser sp = new StateYAMLParser(d);

		OptimalSelection os = new OptimalSelection();

		List <String> userPaths = os.userDirectoryPaths(path);

		OptimalChecker oc = new OptimalChecker(d);

		for(String up : userPaths){
			List <CommandEpisode> ces = os.getCommandEpisodes(up, d, sp);
			System.out.println(up);
			for(CommandEpisode ce : ces){
				int [] isOptimal = oc.actionOptimality(ce.command, ce.episode);
				writeIsOptimal(up, ce.trialId, isOptimal);
			}
		}

	}



}
