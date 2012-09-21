/*
 * File: Yahtzee.java
 * ------------------
 * This program will eventually play the Yahtzee game.
 */

import acm.io.*;
import acm.program.*;
import acm.util.*;

public class Yahtzee extends GraphicsProgram implements YahtzeeConstants {

	public void run() {
		setupPlayers();
		initDisplay();
		playGame();
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
		display = new YahtzeeDisplay(getGCanvas(), playerNames);
	}

	/**
	 * Actually plays a game of Yahtzee.  This is where you should begin writing your
	 * implementation.
	 */
	private void playGame() {
		usedCategories = new boolean[nPlayers][N_CATEGORIES];
		upperScore = new int[nPlayers];
		lowerScore = new int[nPlayers];
		totalScore = new int[nPlayers];

		for (int round = 0; round < N_SCORING_CATEGORIES; round++) {
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
			if (totalScore[player] == winningScore) {
				winner += next + playerNames[player];
			} else if (totalScore[player] > winningScore) {
				winner = playerNames[player];
				winningScore = totalScore[player];				
			} 
			next = " and ";
 		}
		display.printMessage("Congratulations, " + winner + ", you won with a total score of " + winningScore + "!");		
	}

	/**
	 * Wait for a valid selection of a score update that score as well as the
	 * total and sub totals for a particular player
	 * @param player
	 */
	private void updateScore(int player) {
		int category;
		while (true) {
			display.printMessage("Select a category for this roll.");
			category = display.waitForPlayerToSelectCategory();
			if (!usedCategories[player][category]) break;			
		}
		usedCategories[player][category] = true;

		int score = calculateScore(category);
		display.updateScorecard(category, player, score);
		if (category < UPPER_SCORE) {
			upperScore[player] += score;
			display.updateScorecard(UPPER_SCORE, player, upperScore[player]);
		} else {
			lowerScore[player] += score;
			display.updateScorecard(LOWER_SCORE, player, lowerScore[player]);
		}
		totalScore[player] = upperScore[player] + lowerScore[player];
		if (upperScore[player] >= SCORE_UPPER_BONUS_LIMIT) {
			display.updateScorecard(UPPER_BONUS, player, SCORE_UPPER_BONUS);
			totalScore[player] += SCORE_UPPER_BONUS;
		}
		display.updateScorecard(TOTAL, player, totalScore[player]);
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
		display.waitForPlayerToClickRoll(player);
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
		for (int i = 0; i < N_DICE; i++) {
			if (reRollAll || display.isDieSelected(i)) 
				dice[i] = rgen.nextInt(1, N_FACES);
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

	/* Private instance variables */
	private int nPlayers;
	private String[] playerNames;
	private YahtzeeDisplay display;
	private RandomGenerator rgen = new RandomGenerator();
	private int[] dice = new int[N_DICE];
	private boolean[][] usedCategories;
	private int[] upperScore;
	private int[] lowerScore;
	private int[] totalScore;
}
