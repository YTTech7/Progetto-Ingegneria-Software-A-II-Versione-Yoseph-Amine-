package model;

/**
 * Ciclo di vita di una proposta di iniziativa.
 *
 * V2 usa: VALIDA, APERTA
 * V3 aggiunge: CONFERMATA, ANNULLATA, CONCLUSA
 * V4 aggiunge: RITIRATA
 *
 * Gli stati futuri sono già dichiarati per non dover
 * modificare questo file nelle versioni successive.
 */
public enum ProposalState {

    /** Creata e superata la validazione. Solo in memoria, NON su disco. */
    VALIDA,

    /** Pubblicata in bacheca. Salvata su disco. Visibile. */
    APERTA,

    /** (V3) Abbastanza iscritti al termine: evento confermato. */
    CONFERMATA,

    /** (V3) Iscritti insufficienti al termine: evento annullato. */
    ANNULLATA,

    /** (V3) Giorno dopo la data conclusiva. */
    CONCLUSA,

    /** (V4) Ritirata dal configuratore prima dell'evento. */
    RITIRATA;

    /** @return true se la proposta è in uno stato terminale (non modificabile) */
    public boolean isTerminal() {
        return this == ANNULLATA || this == CONCLUSA || this == RITIRATA;
    }

    /** @return true se la proposta è visibile in bacheca */
    public boolean isVisible() {
        return this == APERTA || this == CONFERMATA;
    }
}