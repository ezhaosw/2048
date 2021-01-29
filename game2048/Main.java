package game2048;

import ucb.util.CommandArgs;

import game2048.gui.Game;
import static game2048.Main.Side.*;

/** The main class for the 2048 game.
 *  @author E Zhao
 */
public class Main {

    /** Size of the board: number of rows and of columns. */
    static final int SIZE = 4;
    /** Number of squares on the board. */
    static final int SQUARES = SIZE * SIZE;
    /** Aim of the game. If reached, game ends in victory. */
    static final int GOAL = 2048;

    /** Symbolic names for the four sides of a board. */
    static enum Side { NORTH, EAST, SOUTH, WEST };

    /** The main program.  ARGS may contain the options --seed=NUM,
     *  (random seed); --log (record moves and random tiles
     *  selected.); --testing (take random tiles and moves from
     *  standard input); and --no-display. */
    public static void main(String... args) {
        CommandArgs options =
            new CommandArgs("--seed=(\\d+) --log --testing --no-display",
                            args);
        if (!options.ok()) {
            System.err.println("Usage: java game2048.Main [ --seed=NUM ] "
                               + "[ --log ] [ --testing ] [ --no-display ]");
            System.exit(1);
        }

        Main game = new Main(options);

        while (game.play()) {
            /* No action */
        }
        System.exit(0);
    }

    /** A new Main object using OPTIONS as options (as for main). */
    Main(CommandArgs options) {
        boolean log = options.contains("--log"),
            display = !options.contains("--no-display");
        long seed = !options.contains("--seed") ? 0 : options.getLong("--seed");
        _testing = options.contains("--testing");
        _game = new Game("2048", SIZE, seed, log, display, _testing);
    }

    /** Reset the score for the current game to 0 and clear the board. */
    void clear() {
        _score = 0;
        _count = 0;
        _game.clear();
        _game.setScore(_score, _maxScore);
        for (int r = 0; r < SIZE; r += 1) {
            for (int c = 0; c < SIZE; c += 1) {
                _board[r][c] = 0;
            }
        }
    }

    /** Play one game of 2048, updating the maximum score. Return true
     *  iff play should continue with another game, or false to exit. */
    boolean play() {
        clear();
        setRandomPiece();

        while (true) {

            if (!gameOver()) {
                setRandomPiece();
            }

            if (gameOver()) {
                if (_maxScore < _score) {
                    _maxScore = _score;
                }
                _game.setScore(_score, _maxScore);
                _game.endGame();
            }
        GetMove:
            while (true) {
                String key = _game.readKey();

                switch (key) {
                case "\u2190":
                    key = "Left";
                    break;
                case "\u2191":
                    key = "Up";
                    break;
                case "\u2192":
                    key = "Right";
                    break;
                case "\u2193":
                    key = "Down";
                    break;
                default:
                    break;
                }

                switch (key) {
                case "Up": case "Down": case "Left": case "Right":
                    if (!gameOver() && tiltBoard(keyToSide(key))) {
                        break GetMove;
                    }
                    break;
                case "Quit":
                    return false;
                case "New Game":
                    return true;
                default:
                    break;
                }
            }
            _game.setScore(_score, _maxScore);
        }
    }

    /**
     * Prints the current board.
     */
    private void printBoard() {
        for (int[] row : _board) {
            for (int tile : row) {
                System.out.print(tile);
            }
            System.out.println();
        }
    }

    /** Return true iff the current game is over (no more moves
     *  possible). */
    boolean gameOver() {
        return hasWon() || !canMerge() && _count == SQUARES;
    }

    /**
     * Determines if tiles have matching neighbors.
     *
     * @return true if such neighbors exist; otherwise, return false
     */
    private boolean canMerge() {
        for (int r = 1; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if ((_board[r][c] == _board[(r - 1)][c])
                    || (_board[c][r] == _board[c][(r - 1)])) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks for 2048 tile.
     *
     * @return true if the tile 2048 exists
     */
    private boolean hasWon() {
        for (int[] row : this._board) {
            for (int tile : row) {
                if (tile == GOAL) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Add a tile to a random, empty position, choosing a value (2 or
     *  4) at random.  Has no effect if the board is currently full. */
    void setRandomPiece() {
        int[] tileInfo;
        int value, row, col;

        while (_count != SQUARES) {
            tileInfo = _game.getRandomTile();
            value = tileInfo[0];
            row = tileInfo[1];
            col = tileInfo[2];

            if (_board[row][col] == 0) {
                _game.addTile(value, row, col);
                _board[row][col] = value;
                _count++;
                break;
            }
        }
    }

    /** 
     *  Perform the result of tilting the board toward SIDE.
     *
     *  @param side the side towards which to tilt the board
     *  @return true iff the tilt changes the board. 
     **/
    boolean tiltBoard(Side side) {
        /* As a suggestion (see the project text), you might try copying
         * the board to a local array, turning it so that edge SIDE faces
         * north.  That way, you can re-use the same logic for all
         * directions.  (As usual, you don't have to). */
        int[][] board = new int[SIZE][SIZE];
        int[][] tiles = new int[SIZE][SIZE];
        boolean changed = false;

        for (int r = 0; r < SIZE; r += 1) {
            for (int c = 0; c < SIZE; c += 1) {
                board[r][c] =
                    _board[tiltRow(side, r, c)][tiltCol(side, r, c)];
                if (board[r][c] > 0) {
                    tiles[r][c] = 1;
                }

            }
        }

        for (int r = 1; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                boolean merged = false;

                if (board[r][c] > 0) {
                    int tRow = r - 1;
                    while (tRow >= 0) {
                        if (tiles[tRow][c] == 1
                                && (board[tRow][c] == board[r][c])) {
                            merge(board, side, r, tRow, c, tiles);
                            merged = true;
                            changed = true;
                        } else if (board[tRow][c] != 0) {
                            break;
                        }
                        tRow--;
                    }
                    tRow++;
                    if (!merged && r != tRow) {
                        move(board, side, r, tRow, c, tiles);
                        changed = true;
                    }
                }
            }
        }

        if (!changed) {
            return false;
        }

        for (int r = 0; r < SIZE; r += 1) {
            for (int c = 0; c < SIZE; c += 1) {
                _board[tiltRow(side, r, c)][tiltCol(side, r, c)]
                    = board[r][c];
            }
        }
        _game.setScore(_score, _maxScore);
        _game.displayMoves();
        return true;
    }

    /**
     * Moves the tile at (r0, c) to (r1, c).
     *
     * @param board the rotated board
     * @param side  the current SIDE
     * @param r0    the initial row
     * @param r1    the destination row
     * @param c     the current column
     * @param tiles the array of existing tiles
     */
    private void move(int[][] board, Side side, int r0, int r1, int c,
            int[][] tiles) {
        int tiltedRow0 = tiltRow(side, r0, c);
        int tiltedCol0 = tiltCol(side, r0, c);
        int tiltedRow1 = tiltRow(side, r1, c);
        int tiltedCol1 = tiltCol(side, r1, c);

        board[r1][c] = board[r0][c];
        board[r0][c] = 0;
        tiles[r0][c] = 0;
        tiles[r1][c] = 1;


        _game.moveTile(board[r1][c], tiltedRow0, tiltedCol0,
            tiltedRow1, tiltedCol1);
    }

    /**
     * Merges the tile at (r0, c) with (r1, c). Sets tile at (r1, c) to -1
     * to mark as merged.
     *
     * @param board the rotated board
     * @param side  the current SIDE
     * @param r0    the initial row
     * @param r1    the destination row
     * @param c     the current column
     * @param tiles the array of existing tiles
     */
    private void merge(int[][] board, Side side, int r0, int r1, int c,
            int[][] tiles) {
        board[r1][c] *= 2;
        tiles[r1][c] = -1;

        int tiltedRow0 = tiltRow(side, r0, c);
        int tiltedCol0 = tiltCol(side, r0, c);
        int tiltedRow1 = tiltRow(side, r1, c);
        int tiltedCol1 = tiltCol(side, r1, c);

        _game.mergeTile(board[r0][c], board[r1][c], tiltedRow0,
                tiltedCol0, tiltedRow1, tiltedCol1);

        board[r0][c] = 0;
        tiles[r0][c] = 0;

        _score += board[r1][c];
        _count--;
    }

    /** Return the row number on a playing board that corresponds to row R
     *  and column C of a board turned so that row 0 is in direction SIDE (as
     *  specified by the definitions of NORTH, EAST, etc.).  So, if SIDE
     *  is NORTH, then tiltRow simply returns R (since in that case, the
     *  board is not turned).  If SIDE is WEST, then column 0 of the tilted
     *  board corresponds to row SIZE - 1 of the untilted board, and
     *  tiltRow returns SIZE - 1 - C. */
    int tiltRow(Side side, int r, int c) {
        switch (side) {
        case NORTH:
            return r;
        case EAST:
            return c;
        case SOUTH:
            return SIZE - 1 - r;
        case WEST:
            return SIZE - 1 - c;
        default:
            throw new IllegalArgumentException("Unknown direction");
        }
    }

    /** Return the column number on a playing board that corresponds to row
     *  R and column C of a board turned so that row 0 is in direction SIDE
     *  (as specified by the definitions of NORTH, EAST, etc.). So, if SIDE
     *  is NORTH, then tiltCol simply returns C (since in that case, the
     *  board is not turned).  If SIDE is WEST, then row 0 of the tilted
     *  board corresponds to column 0 of the untilted board, and tiltCol
     *  returns R. */
    int tiltCol(Side side, int r, int c) {
        switch (side) {
        case NORTH:
            return c;
        case EAST:
            return SIZE - 1 - r;
        case SOUTH:
            return SIZE - 1 - c;
        case WEST:
            return r;
        default:
            throw new IllegalArgumentException("Unknown direction");
        }
    }

    /** Return the side indicated by KEY ("Up", "Down", "Left",
     *  or "Right"). */
    Side keyToSide(String key) {
        switch (key) {
        case "Up":
            return NORTH;
        case "Down":
            return SOUTH;
        case "Left":
            return WEST;
        case "Right":
            return EAST;
        default:
            throw new IllegalArgumentException("unknown key designation");
        }
    }

    /** Represents the board: _board[r][c] is the tile value at row R,
     *  column C, or 0 if there is no tile there. */
    private final int[][] _board = new int[SIZE][SIZE];

    /** True iff --testing option selected. */
    private boolean _testing;
    /** THe current input source and output sink. */
    private Game _game;
    /** The score of the current game, and the maximum final score
     *  over all games in this session. */
    private int _score, _maxScore;
    /** Number of tiles on the board. */
    private int _count;
}
