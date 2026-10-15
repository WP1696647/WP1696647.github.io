import java.util.Random;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class JavaProbabilityProject {
    private static final int DOOR_COUNT = 3;
    private static final int TRAINING_ROUNDS = 20_000;
    private static final int GAME_ROUNDS = 10;

    private enum Move {
        STAY,
        SWITCH
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GameWindow().show());
    }

    private static LearningAgent trainAi(Random random) {
        LearningAgent agent = new LearningAgent();
        for (int round = 0; round < TRAINING_ROUNDS; round++) {
            int prizeDoor = random.nextInt(DOOR_COUNT);
            int startingDoor = random.nextInt(DOOR_COUNT);
            int hostDoor = revealGoatDoor(random, prizeDoor, startingDoor);
            int remainingDoor = findRemainingDoor(startingDoor, hostDoor);

            double explorationRate = Math.max(0.02, 1.0 - (double) round / TRAINING_ROUNDS);
            Move move = agent.chooseMove(random, explorationRate);
            int finalDoor = finalDoor(startingDoor, remainingDoor, move);
            agent.learn(move, finalDoor == prizeDoor);
        }
        return agent;
    }

    private static double[] calculateExactWinProbabilities() {
        int[] wins = new int[Move.values().length];
        int totalCases = 0;

        for (int prizeDoor = 0; prizeDoor < DOOR_COUNT; prizeDoor++) {
            for (int startingDoor = 0; startingDoor < DOOR_COUNT; startingDoor++) {
                int hostDoor = findHostDoor(prizeDoor, startingDoor);
                totalCases++;
                int remainingDoor = findRemainingDoor(startingDoor, hostDoor);
                if (finalDoor(startingDoor, remainingDoor, Move.STAY) == prizeDoor) {
                    wins[Move.STAY.ordinal()]++;
                }
                if (finalDoor(startingDoor, remainingDoor, Move.SWITCH) == prizeDoor) {
                    wins[Move.SWITCH.ordinal()]++;
                }
            }
        }

        double[] probabilities = new double[Move.values().length];
        for (Move move : Move.values()) {
            probabilities[move.ordinal()] = (double) wins[move.ordinal()] / totalCases;
        }
        return probabilities;
    }

    private static int findHostDoor(int prizeDoor, int startingDoor) {
        for (int door = 0; door < DOOR_COUNT; door++) {
            if (door != prizeDoor && door != startingDoor) {
                return door;
            }
        }
        throw new IllegalArgumentException("The host must open a different door.");
    }

    private static int revealGoatDoor(Random random, int prizeDoor, int startingDoor) {
        int[] eligibleDoors = new int[DOOR_COUNT];
        int eligibleCount = 0;
        for (int door = 0; door < DOOR_COUNT; door++) {
            if (door != prizeDoor && door != startingDoor) {
                eligibleDoors[eligibleCount++] = door;
            }
        }
        return eligibleDoors[random.nextInt(eligibleCount)];
    }

    private static int findRemainingDoor(int startingDoor, int hostDoor) {
        for (int door = 0; door < DOOR_COUNT; door++) {
            if (door != startingDoor && door != hostDoor) {
                return door;
            }
        }
        throw new IllegalArgumentException("Starting and host doors must be different.");
    }

    private static int finalDoor(int startingDoor, int remainingDoor, Move move) {
        return move == Move.STAY ? startingDoor : remainingDoor;
    }

    private static final class GameWindow {
        private final Random random = new Random();
        private final LearningAgent ai = trainAi(random);
        private final double[] exactProbabilities = calculateExactWinProbabilities();
        private final JFrame frame = new JFrame("Monty Hall Probability Duel");
        private final JButton[] doorButtons = new JButton[DOOR_COUNT];
        private final JButton stayButton = new JButton("Stay");
        private final JButton switchButton = new JButton("Switch");
        private final JButton nextButton = new JButton("Next round");
        private final JLabel roundLabel = new JLabel("", SwingConstants.CENTER);
        private final JLabel scoreLabel = new JLabel("", SwingConstants.CENTER);
        private final JTextArea messageArea = new JTextArea();
        private int round = 1;
        private int prizeDoor;
        private int startingDoor = -1;
        private int hostDoor = -1;
        private int remainingDoor = -1;
        private Move aiMove;
        private int playerWins;
        private int aiWins;
        private int ties;

        private GameWindow() {
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(700, 570);
            frame.setMinimumSize(new Dimension(600, 500));
            frame.setLocationRelativeTo(null);

            JLabel title = new JLabel("Monty Hall Probability Duel", SwingConstants.CENTER);
            title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
            title.setBorder(BorderFactory.createEmptyBorder(18, 12, 8, 12));

            roundLabel.setFont(roundLabel.getFont().deriveFont(Font.BOLD, 16f));
            scoreLabel.setFont(scoreLabel.getFont().deriveFont(Font.BOLD, 16f));

            messageArea.setEditable(false);
            messageArea.setLineWrap(true);
            messageArea.setWrapStyleWord(true);
            messageArea.setFont(messageArea.getFont().deriveFont(14f));
            messageArea.setText(
                    "A prize is hidden behind one of three doors. Choose a door to begin. "
                    + "The host will reveal a goat behind another door, then you can stay "
                    + "with your choice or switch.\n\n"
                    + "The AI learned by trying both strategies in simulated games. "
                    + "Exact enumeration shows staying wins 1/3 of the time and switching "
                    + "wins 2/3 of the time.");
            messageArea.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

            JPanel doorsPanel = new JPanel(new GridLayout(1, DOOR_COUNT, 12, 0));
            doorsPanel.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
            for (int i = 0; i < DOOR_COUNT; i++) {
                final int door = i;
                doorButtons[i] = new JButton("Door " + (i + 1));
                doorButtons[i].setFont(doorButtons[i].getFont().deriveFont(Font.BOLD, 18f));
                doorButtons[i].addActionListener(event -> chooseStartingDoor(door));
                doorsPanel.add(doorButtons[i]);
            }

            JPanel choicesPanel = new JPanel(new GridLayout(1, 3, 12, 0));
            choicesPanel.setBorder(BorderFactory.createEmptyBorder(4, 24, 18, 24));
            stayButton.addActionListener(event -> finishRound(Move.STAY));
            switchButton.addActionListener(event -> finishRound(Move.SWITCH));
            nextButton.addActionListener(event -> nextRound());
            stayButton.setEnabled(false);
            switchButton.setEnabled(false);
            nextButton.setEnabled(false);
            choicesPanel.add(stayButton);
            choicesPanel.add(switchButton);
            choicesPanel.add(nextButton);

            JPanel top = new JPanel(new BorderLayout());
            top.add(title, BorderLayout.NORTH);
            top.add(roundLabel, BorderLayout.CENTER);
            top.add(scoreLabel, BorderLayout.SOUTH);

            frame.add(top, BorderLayout.NORTH);
            frame.add(messageArea, BorderLayout.CENTER);
            JPanel bottom = new JPanel(new BorderLayout());
            bottom.add(doorsPanel, BorderLayout.NORTH);
            bottom.add(choicesPanel, BorderLayout.SOUTH);
            frame.add(bottom, BorderLayout.SOUTH);
            updateScore();
            frame.setResizable(true);
        }

        private void show() {
            beginRound();
            frame.setVisible(true);
        }

        private void beginRound() {
            startingDoor = -1;
            hostDoor = -1;
            remainingDoor = -1;
            prizeDoor = random.nextInt(DOOR_COUNT);
            aiMove = null;
            roundLabel.setText("Round " + round + " of " + GAME_ROUNDS);
            messageArea.setText("Choose a starting door. The AI chooses the same door as you.");
            for (int i = 0; i < DOOR_COUNT; i++) {
                doorButtons[i].setText("Door " + (i + 1));
                doorButtons[i].setEnabled(true);
            }
            stayButton.setEnabled(false);
            switchButton.setEnabled(false);
            nextButton.setEnabled(false);
            nextButton.setText("Next round");
        }

        private void chooseStartingDoor(int door) {
            startingDoor = door;
            hostDoor = revealGoatDoor(random, prizeDoor, startingDoor);
            remainingDoor = findRemainingDoor(startingDoor, hostDoor);
            aiMove = ai.chooseBestMove();

            doorButtons[startingDoor].setText("Your choice");
            doorButtons[hostDoor].setText("Goat revealed");
            doorButtons[hostDoor].setEnabled(false);
            doorButtons[remainingDoor].setText("Other door");
            for (JButton doorButton : doorButtons) {
                doorButton.setEnabled(false);
            }
            stayButton.setEnabled(true);
            switchButton.setEnabled(true);
            messageArea.setText("The host opened door " + (hostDoor + 1) + " and revealed a goat.\n"
                    + "The AI chooses to " + aiMove.name().toLowerCase() + ". "
                    + "Will you stay with door " + (startingDoor + 1) + " or switch to door "
                    + (remainingDoor + 1) + "?");
        }

        private void finishRound(Move playerMove) {
            int playerFinalDoor = finalDoor(startingDoor, remainingDoor, playerMove);
            int aiFinalDoor = finalDoor(startingDoor, remainingDoor, aiMove);
            boolean playerWon = playerFinalDoor == prizeDoor;
            boolean aiWon = aiFinalDoor == prizeDoor;

            if (playerWon && aiWon) {
                ties++;
                messageArea.setText("You both found the prize! This round is a tie.");
            } else if (playerWon) {
                playerWins++;
                messageArea.setText("You found the prize and won this round!");
            } else if (aiWon) {
                aiWins++;
                messageArea.setText("The AI found the prize and won this round.");
            } else {
                ties++;
                messageArea.setText("Neither strategy found the prize. This round is a tie.");
            }

            doorButtons[prizeDoor].setText("Prize!");
            for (JButton doorButton : doorButtons) {
                doorButton.setEnabled(false);
            }
            stayButton.setEnabled(false);
            switchButton.setEnabled(false);
            updateScore();

            String explanation = playerMove == Move.STAY
                    ? " Your original door had a 1/3 chance; switching would have had a 2/3 chance."
                    : " Your original door had a 1/3 chance; switching used the combined 2/3 chance of the other doors.";
            messageArea.append("\nThe prize was behind door " + (prizeDoor + 1) + "." + explanation);
            messageArea.append("\nAI estimates after " + TRAINING_ROUNDS + " training games: stay "
                    + String.format("%.1f%%", ai.getEstimatedWinRate(Move.STAY) * 100)
                    + ", switch " + String.format("%.1f%%", ai.getEstimatedWinRate(Move.SWITCH) * 100) + ".");

            if (round == GAME_ROUNDS) {
                messageArea.append("\n\nGame complete! Final score — You: " + playerWins
                        + ", AI: " + aiWins + ", ties: " + ties + ".");
                nextButton.setText("Play again");
            }
            nextButton.setEnabled(true);
        }

        private void nextRound() {
            if (round == GAME_ROUNDS) {
                round = 1;
                playerWins = 0;
                aiWins = 0;
                ties = 0;
                updateScore();
            } else {
                round++;
            }
            beginRound();
        }

        private void updateScore() {
            scoreLabel.setText("You: " + playerWins + "   |   AI: " + aiWins + "   |   Ties: " + ties);
        }
    }

    private static final class LearningAgent {
        private final double[] estimatedWinRates = new double[Move.values().length];
        private final int[] attempts = new int[Move.values().length];

        private Move chooseMove(Random random, double explorationRate) {
            if (random.nextDouble() < explorationRate) {
                return Move.values()[random.nextInt(Move.values().length)];
            }
            return chooseBestMove();
        }

        private Move chooseBestMove() {
            if (estimatedWinRates[Move.STAY.ordinal()] == estimatedWinRates[Move.SWITCH.ordinal()]) {
                return Move.SWITCH;
            }
            return estimatedWinRates[Move.SWITCH.ordinal()] > estimatedWinRates[Move.STAY.ordinal()]
                    ? Move.SWITCH : Move.STAY;
        }

        private void learn(Move move, boolean won) {
            int index = move.ordinal();
            attempts[index]++;
            estimatedWinRates[index] += ((won ? 1.0 : 0.0) - estimatedWinRates[index]) / attempts[index];
        }

        private double getEstimatedWinRate(Move move) {
            return estimatedWinRates[move.ordinal()];
        }
    }
}
