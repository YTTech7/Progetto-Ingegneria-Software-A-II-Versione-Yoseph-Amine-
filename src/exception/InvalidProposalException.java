package exception;

/**
 * Lanciata da ProposalService quando una proposta non supera
 * la validazione. Il messaggio descrive quale regola è fallita.
 *
 * Regole di validazione:
 *   1. Tutti i campi obbligatori devono essere compilati
 *   2. Termine iscrizione > data corrente
 *   3. Data evento > Termine iscrizione + 2 giorni
 */
public class InvalidProposalException extends DomainException {

    public InvalidProposalException(String reason) {
        super("Proposta non valida: " + reason);
    }
}