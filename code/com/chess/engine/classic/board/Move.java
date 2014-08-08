package com.chess.engine.classic.board;

import com.chess.engine.classic.board.Board.Builder;
import com.chess.engine.classic.pieces.Pawn;
import com.chess.engine.classic.pieces.Piece;
import com.chess.engine.classic.pieces.Piece.Type;
import com.chess.engine.classic.pieces.Rook;
import com.google.common.collect.Iterables;

public class Move {

    protected final Board board;
    protected final int currentCoordinate;
    protected final int destinationCoordinate;
    protected final Piece movedPiece;
    protected final boolean isFirstMove;

    public Move(final Board board,
                final int currentCoordinate,
                final int destinationCoordinate,
                final Piece pieceMoved) {
        this.board = board;
        this.currentCoordinate = currentCoordinate;
        this.destinationCoordinate = destinationCoordinate;
        this.movedPiece = pieceMoved;
        this.isFirstMove = pieceMoved.isFirstMove();
    }

    public Move(final Board board,
                final int currentCoordinate,
                final int destinationCoordinate) {
        this.board = board;
        this.currentCoordinate = currentCoordinate;
        this.destinationCoordinate = destinationCoordinate;
        this.movedPiece = null;
        this.isFirstMove = false;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.currentCoordinate;
        result = prime * result + this.destinationCoordinate;
        result = prime * result
                + ((this.movedPiece == null) ? 0 : this.movedPiece.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object aThat) {
        if (this == aThat) {
            return true;
        }
        if (!(aThat instanceof Move) ) {
            return false;
        }
        final Move that = (Move)aThat;

        return((getCurrentCoordinate() == that.getCurrentCoordinate()) &&
               (getDestinationCoordinate() == that.getDestinationCoordinate()) &&
               (getMovedPiece().equals(that.getMovedPiece())));
    }

    public Board getBoard() {
        return this.board;
    }

    public int getCurrentCoordinate() {
        return this.currentCoordinate;
    }

    public int getDestinationCoordinate() {
        return this.destinationCoordinate;
    }

    public Piece getMovedPiece() {
        return this.movedPiece;
    }

    public boolean isAttack() {
        return false;
    }

    public boolean isCastle() {
        return false;
    }

    public Piece getAttackedPiece() {
        return null;
    }

    public Board execute() {

        final Board.Builder builder = new Builder();

        for(final Piece piece : board.getAllPieces()) {
            if(!this.movedPiece.equals(piece)) {
                builder.setPiece(piece.getPiecePosition(), piece);
            }
        }

        builder.setPiece(this.destinationCoordinate, this.movedPiece.movePiece(this));
        builder.setMoveMaker(this.board.currentPlayer().getOpponent().getAlliance());

        return builder.build();
    }

    public Board undo() {
        final Board.Builder builder = new Builder();

        for(final Piece piece : this.board.getAllPieces()) {
            builder.setPiece(piece.getPiecePosition(), piece);
        }

        builder.setMoveMaker(this.board.currentPlayer().getAlliance());

        return builder.build();
    }

    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder(Board.getPositionAtCoordinate(this.currentCoordinate));
        s.append("-");
        s.append(Board.getPositionAtCoordinate(this.destinationCoordinate));
        return s.toString();
    }

    public static class PawnPromotion extends Move {

        final Move decoratedMove;
        final Pawn promotedPawn;

        public PawnPromotion(final Move decoratedMove) {
            super(decoratedMove.getBoard(),
                  decoratedMove.getCurrentCoordinate(),
                  decoratedMove.getDestinationCoordinate(),
                  decoratedMove.getMovedPiece());
            this.decoratedMove = decoratedMove;
            this.promotedPawn = (Pawn)decoratedMove.getMovedPiece();
        }

        @Override
        public int hashCode() {
            return decoratedMove.hashCode() + (31 * promotedPawn.hashCode());
        }

        @Override
        public boolean equals(final Object aThat) {
            return this == aThat || aThat instanceof PawnPromotion && (super.equals(aThat));
        }

        @Override
        public Board execute() {

            final Board pawnMovedBoard = this.decoratedMove.execute();
            final Board.Builder builder = new Builder();

            for (final Piece piece : pawnMovedBoard.getAllPieces()) {
                if (!this.promotedPawn.equals(piece)) {
                    builder.setPiece(piece.getPiecePosition(), piece);
                }
            }

            builder.setPiece(this.destinationCoordinate, this.promotedPawn.getPromotionPiece());
            builder.setMoveMaker(pawnMovedBoard.currentPlayer().getAlliance());

            return builder.build();

        }

        @Override
        public boolean isAttack() {
            return decoratedMove.isAttack();
        }

        @Override
        public Piece getAttackedPiece() {
            return this.decoratedMove.getAttackedPiece();
        }

        @Override
        public String toString() {
            final StringBuilder s = new StringBuilder(Board.getPositionAtCoordinate(this.currentCoordinate));
            s.append("-");
            s.append(Board.getPositionAtCoordinate(this.destinationCoordinate));
            s.append("=");
            s.append(Type.QUEEN.toString());
            return s.toString();
        }

    }

    public static class PawnJump extends Move {

        public PawnJump(final Board board,
                        final int currentCoordinate,
                        final int destinationCoordinate,
                        final Pawn pieceMoved) {
            super(board, currentCoordinate, destinationCoordinate, pieceMoved);
        }

        @Override
        public boolean equals(final Object aThat) {
            return this == aThat || aThat instanceof PawnJump && (super.equals(aThat));
        }

    }

    static abstract class CastleMove extends Move {

        protected final Rook castleRook;
        protected  final int castleRookStart;
        protected final int castleRookDestination;

        CastleMove(final Board board,
                   final int currentCoordinate,
                   final int destinationCoordinate,
                   final Piece pieceMoved,
                   final Rook castleRook,
                   final int castleRookStart,
                   final int castleRookDestination) {
            super(board, currentCoordinate, destinationCoordinate, pieceMoved);
            this.castleRook = castleRook;
            this.castleRookStart = castleRookStart;
            this.castleRookDestination = castleRookDestination;
        }

        public Rook getCastleRook() {
            return this.castleRook;
        }

        @Override
        public boolean isCastle() {
            return true;
        }

        @Override
        public Board execute() {
            final Board.Builder builder = new Builder();

            for(final Piece piece : this.board.getAllPieces()) {
                if(!this.movedPiece.equals(piece) && !this.castleRook.equals(piece)) {
                    builder.setPiece(piece.getPiecePosition(), piece);
                }
            }

            builder.setPiece(this.destinationCoordinate, this.movedPiece.movePiece(this));
            //calling movePiece here doesn't work, we need to explicitly create a new Rook
            builder.setPiece(this.castleRookDestination,
                    new Rook(this.castleRook.getPieceAllegiance(), this.castleRookDestination, false));
            builder.setMoveMaker(this.board.currentPlayer().getOpponent().getAlliance());

            return builder.build();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + this.castleRook.hashCode();
            result = prime * result + this.castleRookDestination;
            return result;
        }

        @Override
        public boolean equals(final Object aThat) {
            if (this == aThat) {
                return true;
            }
            if (!(aThat instanceof CastleMove) ) {
                return false;
            }
            final CastleMove that = (CastleMove)aThat;
            return(super.equals(that) && this.castleRook.equals(that.getCastleRook()));
        }

    }

    public static class KingSideCastleMove extends CastleMove {


        public KingSideCastleMove(final Board board,
                                  final int currentCoordinate,
                                  final int destinationCoordinate,
                                  final Piece pieceMoved,
                                  final Rook castleRook,
                                  final int castleRookStart,
                                  final int castleRookDestination) {
            super(board,
                  currentCoordinate,
                  destinationCoordinate,
                  pieceMoved,
                  castleRook,
                  castleRookStart,
                  castleRookDestination);
        }

        @Override
        public boolean equals(final Object aThat) {
            if (this == aThat) {
                return true;
            }
            if (!(aThat instanceof KingSideCastleMove)) {
                return false;
            }
            final KingSideCastleMove that = (KingSideCastleMove)aThat;
            return(super.equals(that) && this.castleRook.equals(that.getCastleRook()));
        }

        @Override
        public String toString() {
            return "0-0";
        }

    }

    public static class QueenSideCastleMove extends CastleMove {

        public QueenSideCastleMove(final Board board,
                                   final int currentCoordinate,
                                   final int destinationCoordinate,
                                   final Piece pieceMoved,
                                   final Rook castleRook,
                                   final int castleRookStart,
                                   final int rookCastleDestination) {
            super(board,
                  currentCoordinate,
                  destinationCoordinate,
                  pieceMoved,
                  castleRook,
                  castleRookStart,
                  rookCastleDestination);
        }

        @Override
        public boolean equals(final Object aThat) {
            if (this == aThat) {
                return true;
            }
            if (!(aThat instanceof QueenSideCastleMove) ) {
                return false;
            }
            final QueenSideCastleMove that = (QueenSideCastleMove)aThat;
            return(super.equals(that) && castleRook.equals(that.getCastleRook()));
        }

        @Override
        public String toString() {
            return "0-0-0";
        }

    }

    public static class AttackMove extends Move {

        private final Piece attackedPiece;

        public AttackMove(final Board board,
                          final int currentCoordinate,
                          final int destinationCoordinate,
                          final Piece pieceMoved,
                          final Piece pieceAttacked) {
            super(board, currentCoordinate, destinationCoordinate, pieceMoved);
            this.attackedPiece = pieceAttacked;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + this.currentCoordinate;
            result = prime * result + this.destinationCoordinate;
            result = prime * result
                    + ((this.movedPiece == null) ? 0 : this.movedPiece.hashCode());
            result = prime * result + this.attackedPiece.hashCode();
            return result;
        }

        @Override
        public boolean equals(final Object aThat) {
            if (this == aThat) {
                return true;
            }
            if (!(aThat instanceof AttackMove)) {
                return false;
            }
            final AttackMove that = (AttackMove)aThat;
            return (super.equals(that) && (getAttackedPiece().equals(that.getAttackedPiece())));
        }

        @Override
        public String toString() {
            final StringBuilder s = new StringBuilder(Board.getPositionAtCoordinate(this.currentCoordinate));
            s.append("x");
            s.append(Board.getPositionAtCoordinate(this.destinationCoordinate));
            return s.toString();
        }

        @Override
        public Piece getAttackedPiece() {
            return this.attackedPiece;
        }

        @Override
        public boolean isAttack() {
            return true;
        }

    }

    static class NullMove extends Move {

        public NullMove(final Board board,
                        final int currentCoordinate,
                        final int destinationCoordinate) {
            super(board, currentCoordinate, destinationCoordinate);
        }

        @Override
        public Board execute() {
            throw new RuntimeException("cannot execute null move!");
        }

        @Override
        public String toString() {
            final StringBuilder s = new StringBuilder(Board.getPositionAtCoordinate(this.currentCoordinate));
            s.append("-");
            s.append(Board.getPositionAtCoordinate(this.destinationCoordinate));
            return s.toString();
        }
    }

        public static class MoveFactory {

        private MoveFactory() {
        }

        public static Move createMove(final Board board,
                                      final int currentCoordinate,
                                      final int destinationCoordinate) {
            for(final Move m : Iterables.concat(board.whitePlayer().getLegalMoves(), board.blackPlayer().getLegalMoves())) {
                if(m.getCurrentCoordinate() == currentCoordinate && m.getDestinationCoordinate() == destinationCoordinate) {
                    return m;
                }
            }
            return new NullMove(board, currentCoordinate, destinationCoordinate);
        }
    }
}