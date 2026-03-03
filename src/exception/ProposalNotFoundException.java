package exception;

/**
 * Lanciata da ProposalService quando una proposta referenziata
 * tramite ID non viene trovata in ApplicationState.
 */
public class ProposalNotFoundException extends DomainException {

    public ProposalNotFoundException(String id) {
        super("Proposta non trovata con ID: '" + id + "'.");
    }
}