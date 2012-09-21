/*
 * File: TrippleYahtzee.java
 * ------------------
 * This program will eventually play the Yahtzee game.
 */

import java.awt.event.MouseEvent;
import java.io.*;
import java.util.*;

import acm.io.*;
import acm.program.*;
import acm.util.*;

public class TrippleYahtzee extends GraphicsProgram implements YahtzeeConstants {

	public void run() {
		showHighScores();
		setupPlayers();
		initDisplay();
		playGame();
	}

	/**
	 * Show high scores of previous games
	 */
	private void showHighScores() {		
		String text = "Highscores:\n";
		loadHighScores();

		if (highScoreNames.size() == 0) {
			text += "none available yet";
		} else {
			for (int i = 0; i < highScoreNames.size(); i++) {
				int value = highScoreValues.get(i);
				if (value / 100 == 0) text += "  ";
				if (value / 10 == 0) text += "  ";
				text += value + " " + highScoreNames.get(i) + "\n";
			}
		}

		IODialog dialog = getDialog();
		dialog.println(text);		
	}

	/**
	 * load high scores from file
	 */
	private void loadHighScores() {
		highScoreNames = new ArrayList<String>();
		highScoreValues = new ArrayList<Integer>();
		File file = new File(HIGHSCORE_FILE);
		try {
			Scanner scanner = new Scanner(new FileReader(file));
			while (scanner.hasNextLine()) {
				processHighScoresLine(scanner.nextLine());
			}
			scanner.close();
		} catch (IOException ex) {
			// don't care do nothing
		}
	}

	/**
	 * parse single line of high score file
	 * @param line
	 */
	protected void processHighScoresLine(String line){
		Scanner scanner = new Scanner(line);
		scanner.useDelimiter("=");
		if (scanner.hasNext()) {
			highScoreValues.add(new Integer(scanner.next()));
			highScoreNames.add(scanner.next());
		}
	}

	/** 
	 * save high scores to file
	 */
	private void saveHighScores() {
		try {
			PrintWriter printWriter = new PrintWriter(new FileWriter(HIGHSCORE_FILE));
			for (int i = 0; i < highScoreNames.size(); i++) {
				printWriter.println(highScoreValues.get(i) + "=" + highScoreNames.get(i));
			}
			printWriter.close();
		} catch (IOException ex) {
			throw new ErrorException(ex);
		}
	}

	/**
	 * check for new high scores and return if any have been found
	 * @return
	 */
	private boolean newHighScore() {
		boolean fileNeedsUpdate = false;
		for (int player = 0; player < nPlayers; player++) {
			boolean newHighScore = false;
			int total = 0; 
			for (int col = 0; col < COLS_PER_PLAYER; col++) {
				total += totalScore[player][col];
			}
			for (int i = highScoreNames.size() - 1; i >= 0; i--) {				
				if (highScoreValues.get(i).intValue() <= total) {
					newHighScore = true;
					fileNeedsUpdate = true;
					if (i == 0) {
						addHighScore(i, player, total);
					}
				} else {
					if (newHighScore) {
						addHighScore(i + 1, player, total);						
					}
					break;
				}

			}
		}
		if (fileNeedsUpdate) saveHighScores();

		return fileNeedsUpdate;
	}

	/**
	 * add a highscore from player at given position 
	 * @param i
	 * @param player
	 * @param total
	 */
	private void addHighScore(int i, int player, int total) {
		highScoreNames.add(i, playerNames[player]);
		highScoreValues.add(i, new Integer(total));
		if (highScoreNames.size() > N_HIGHSCORES) {
			highScoreNames.remove(N_HIGHSCORES);
			highScoreValues.remove(N_HIGHSCORES);
		}
	}

	/**
	 * Prompts the user for information about the number of players, then sets up the
	 * players array and number of players.
	 */
	private void setupPlayers() {
		nPlayers = chooseNumberOfPlayers();
		//nPlayers = 2;

		/* Set up the players array by reading names for each player. */
		playerNames = new String[nPlayers];
		for (int i = 0; i < nPlayers; i++) {
			/* IODialog is a class that allows us to prompt the user for information as a
			 * series of dialog boxes.  We will use this here to read player names.
			 */
			IODialog dialog = getDialog();
			playerNames[i] = dialog.readLine("Enter name for player " + (i + 1));
			//playerNames[i] = "Player " + i;
		}
	}

	/**
	 * Prompts the user for a number of players in this game, reprompting until the user
	 * enters a valid number.
	 * 
	 * @return The number of players in this game.
	 */
	private int chooseNumberOfPlayers() {
		/* See setupPlayers() for more details on how IODialog works. */
		IODialog dialog = getDialog();

		while (true) {
			/* Prompt the user for a number of players. */
			int result = dialog.readInt("Enter number of players");

			/* If the result is valid, return it. */
			if (result > 0 && result <= MAX_PLAYERS)
				return result;

			dialog.println("Please enter a valid number of players.");
		}
	}

	/**
	 * Sets up the YahtzeeDisplay associated with this game.
	 */
	private void initDisplay() {
		String[] colNames = new String[nPlayers * COLS_PER_PLAYER];
		for (int i = 0; i < nPlayers; i++) {
			for (int j = 0; j < COLS_PER_PLAYER; j++) {
				colNames[i * COLS_PER_PLAYER + j] = playerNames[i] + "*" + (j + 1);
			}
		}		
		display = new YahtzeeDisplay(getGCanvas(), colNames);
	}

	/**
	 * Actually plays a game of Yahtzee.  This is where you should begin writing your
	 * implementation.
	 */
	private void playGame() {
		usedCategories = new boolean[nPlayers][COLS_PER_PLAYER][N_CATEGORIES];
		upperScore = new int[nPlayers][COLS_PER_PLAYER];
		lowerScore = new int[nPlayers][COLS_PER_PLAYER];
		totalScore = new int[nPlayers][COLS_PER_PLAYER];
		allowAdditionalYahtzees = new boolean[nPlayers][COLS_PER_PLAYER];
		for (int player = 0; player < nPlayers; player++) {
			for (int col = 0; col < COLS_PER_PLAYER; col++) {
				allowAdditionalYahtzees[player][col] = true;
			}			
		}		

		for (int round = 0; round < N_SCORING_CATEGORIES * COLS_PER_PLAYER; round++) {
			for (int player = 0; player < nPlayers; player++) {	
				firstRoll(player);
				for (int roll = 1; roll < N_ROLLS; roll++) {					
					furtherRoll();
				}
				updateScore(player);
			}
		}
		findWinner();
	}

	/**
	 * Find winners by looping over the total scores of each player and display
	 * the name[s] of the player with the highest score
	 */
	private void findWinner() {
		String winner = "";
		String next = "";
		int winningScore = 0;
		for (int player = 0; player < nPlayers; player++) {
			int total = 0; 
			for (int col = 0; col < COLS_PER_PLAYER; col++) {
				total += totalScore[player][col];
			}
			if (total == winningScore) {
				winner += next + playerNames[player];
			} else if (total > winningScore) {
				winner = playerNames[player];
				winningScore = total;				
			} 
			next = " and ";
		}
		if (newHighScore()) {
			display.printMessage("Congratulations, " + winner + ", you won with a new high score of " + winningScore + "!");
		} else {
			display.printMessage("Congratulations, " + winner + ", you won with a total score of " + winningScore + "!");
		}
	}

	/**
	 * Wait for a valid selection of a score update that score as well as the
	 * total and sub totals for a particular player
	 * @param player
	 */
	private void updateScore(int player) {
		int category;
		int col;
		addMouseListeners();
		
		while (true) {
			display.printMessage("Select a category for this roll.");
			category = display.waitForPlayerToSelectCategory();
			col = COLS_PER_PLAYER - 1;
			for(int i = 0; i < COLS_PER_PLAYER; i++) {
				if (xMouseClick <= X_FIRST_COL + X_SIZE_COL * (1 + i + COLS_PER_PLAYER * player)) {
					col = i;
					break;
				}
			}
			if (!usedCategories[player][col][category]) break;			
		}
		usedCategories[player][col][category] = true;

		int score = calculateScore(category) * (col + 1);

		if (category == YAHTZEE) {
			if (score == 0) {
				allowAdditionalYahtzees[player][col] = false;
			}			
		} else {
			if (allowAdditionalYahtzees[player][col] 
			    && usedCategories[player][col][YAHTZEE] 
			    && (calculateOfAKindValues(5) > 0)) {
				score += SCORE_ADDITONAL_YAHTZEES;
			}
		}

		int scoreIndex = col + player * COLS_PER_PLAYER;
		display.updateScorecard(category, scoreIndex, score);
		
		if (category < UPPER_SCORE) {
			upperScore[player][col] += score;
			display.updateScorecard(UPPER_SCORE, scoreIndex, upperScore[player][col]);
		} else {
			lowerScore[player][col] += score;
			display.updateScorecard(LOWER_SCORE, scoreIndex, lowerScore[player][col]);
		}
		totalScore[player][col] = upperScore[player][col] + lowerScore[player][col];

		if (upperScore[player][col] >= SCORE_UPPER_BONUS_LIMIT * (col + 1)) {
			display.updateScorecard(UPPER_BONUS, scoreIndex, SCORE_UPPER_BONUS * (col + 1));
			totalScore[player][col] += SCORE_UPPER_BONUS * (col + 1);
		}

		for (col = 0; col < COLS_PER_PLAYER; col++) {
			display.updateScorecard(TOTAL, col + player * COLS_PER_PLAYER, totalScore[player][col]);			
		}
	}

	public void mousePressed(MouseEvent e) {
		xMouseClick = e.getX();
	}
	
	/**
	 * Calculate the score of a chosen category and return the result
	 * @param category
	 * @return
	 */
	private int calculateScore(int category) {
		int score = 0;
		switch (category) {
		case ONES: 
			score = calculateSingleValues(1);
			break;
		case TWOS: 
			score = calculateSingleValues(2);
			break;
		case THREES: 
			score = calculateSingleValues(3);
			break;
		case FOURS: 
			score = calculateSingleValues(4);
			break;
		case FIVES: 
			score = calculateSingleValues(5);
			break;
		case SIXES:
			score = calculateSingleValues(6);
			break;
		case THREE_OF_A_KIND:
			score = calculateOfAKindValues(3);
			break;
		case FOUR_OF_A_KIND:
			score = calculateOfAKindValues(4);
			break;
		case YAHTZEE:
			score = calculateOfAKindValues(5);
			break;
		case CHANCE:
			score = calculateOfAKindValues(0);
			break;
		case FULL_HOUSE:
			score = calculateOfAKindValues(FULL_HOUSE);
			break;
		case SMALL_STRAIGHT:
			score = calculateOfAKindValues(SMALL_STRAIGHT);
			break;
		case LARGE_STRAIGHT:
			score = calculateOfAKindValues(LARGE_STRAIGHT);
			break;			
		default:
			break;
		}
		return score;
	}

	/**
	 * Calculate the score a given number of equal faces and return the result
	 * @param number
	 * @return
	 */
	private int calculateOfAKindValues(int number) {
		int result = 0;
		int value[] = new int[N_FACES];
		boolean isNumber = false;
		for (int i = 0; i < N_DICE; i++) {
			result += dice[i];
			if (++value[dice[i] - 1] >= number) isNumber = true;
		}
		if (number == FULL_HOUSE) {
			boolean found2 = false;
			boolean found3 = false;
			for (int i = 0; i < N_FACES; i ++) {
				if (value[i] == 2) found2 = true;
				if (value[i] == 3) found3 = true;				
			}
			if (found2 && found3) return SCORE_FULL_HOUSE;
		} else if ((number == SMALL_STRAIGHT) || (number == LARGE_STRAIGHT)) {
			int consecutives = 0;
			int maxConsecutives = 0;
			int previous = -1;
			for (int i = 0; i < N_FACES; i ++) {
				if ((value[i] > 0) && (i == previous + 1)) {
					consecutives++;
					if (consecutives > maxConsecutives) 
						maxConsecutives = consecutives;
				} else {
					consecutives = 0;
				}
				previous = i;
			}
			if ((number == SMALL_STRAIGHT) && (maxConsecutives >= 4)) {
				return SCORE_SMALL_STRAIGHT;
			} else if ((number == LARGE_STRAIGHT) && (maxConsecutives == 5)) {
				return SCORE_LARGE_STRAIGHT;
			}

		}
		if (!isNumber) return 0;
		if (number == 5) return SCORE_YAHTZEE;
		return result;
	}

	/** 
	 * Calculate the score for given face value and return the result
	 * @param value
	 * @return
	 */
	private int calculateSingleValues(int value) {
		int result = 0;
		for (int i = 0; i < N_DICE; i++) {
			if (dice[i] == value) result += value;
		}		
		return result;
	}

	/**
	 * Handle the first roll of dice of a particular player
	 * @param player
	 */
	private void firstRoll(int player) {
		String playerName = playerNames[player];				
		display.printMessage(playerName + "'s turn. Click \"Roll Dice\" button to roll the dice.");
		display.waitForPlayerToClickRoll(player * COLS_PER_PLAYER);
		rollDice(true);
		display.displayDice(dice);		
	}

	/** 
	 * Handle consecutive rolls of dice
	 */
	private void furtherRoll() {
		display.printMessage("Select the dice you which to re-roll and click \"Roll Again\".");
		display.waitForPlayerToSelectDice();
		rollDice(false);
		display.displayDice(dice);
	}

	/**
	 * Roll all or selected dice
	 * @param reRollAll
	 */
	private void rollDice(boolean reRollAll) {
		//int allSameValue = rgen.nextInt(1, N_FACES);
		for (int i = 0; i < N_DICE; i++) {
			if (reRollAll || display.isDieSelected(i)) {
				dice[i] = rgen.nextInt(1, N_FACES);
				//dice[i] = allSameValue;
			}				
		}
	}

	/* Private constants */
	private static final int N_ROLLS = 3;
	private static final int N_FACES = 6;
	private static final int SCORE_FULL_HOUSE = 25;
	private static final int SCORE_SMALL_STRAIGHT = 30;
	private static final int SCORE_LARGE_STRAIGHT = 40;
	private static final int SCORE_YAHTZEE = 50;
	private static final int SCORE_UPPER_BONUS_LIMIT = 63;
	private static final int SCORE_UPPER_BONUS = 35;
	private static final String HIGHSCORE_FILE = "HighScores.txt";
	private static final int N_HIGHSCORES = 10;
	private static final int SCORE_ADDITONAL_YAHTZEES = 100;
	
	private static final int COLS_PER_PLAYER = 3;
	private static final int X_FIRST_COL = 252;
	private static final int X_SIZE_COL = 65;


	/* Private instance variables */
	private int nPlayers;
	private String[] playerNames;
	private YahtzeeDisplay display;
	private RandomGenerator rgen = new RandomGenerator();
	private int[] dice = new int[N_DICE];
	private boolean[][][] usedCategories;
	private int[][] upperScore;
	private int[][] lowerScore;
	private int[][] totalScore;
	private ArrayList<String> highScoreNames;
	private ArrayList<Integer> highScoreValues;
	private boolean[][] allowAdditionalYahtzees;
	private int xMouseClick;

}
