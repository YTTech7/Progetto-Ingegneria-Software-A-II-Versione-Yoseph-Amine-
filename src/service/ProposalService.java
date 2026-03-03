package service;

import exception.InvalidProposalException;
import exception.ProposalNotFoundException;
import model.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ProposalService — tutta la business logic delle proposte.
 *
 * Responsabilità:
 *   UC-11: creare una bozza in memoria e validarla (3 regole)
 *   UC-12: pubblicare una proposta valida in bacheca
 *   UC-13: leggere la bacheca per categoria
 *
 * REGOLA FONDAMENTALE (specifica V2):
 *   Le sessioni del configuratore si assumono terminate nello stesso
 *   giorno in cui iniziano. Le proposte VALIDE non pubblicate vengono
 *   perse a fine sessione — non vengono MAI salvate su disco.
 *   Solo le proposte APERTE (dopo publish()) vengono persistite.
 *
 * Invariante: state != null
 */
public class ProposalService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ApplicationState state;

    /**
     * @pre state != null
     */
    public ProposalService(ApplicationState state) {
        assert state != null : "ProposalService requires a non-null ApplicationState";
        this.state = state;
    }

    // ================================================================
    // UC-11: CREAZIONE E VALIDAZIONE BOZZA
    // ================================================================

    /**
     * Crea una nuova bozza di proposta per la categoria data.
     * La bozza esiste solo in memoria — non viene salvata su disco.
     *
     * @param category la categoria dell'iniziativa
     * @return una nuova Proposal con stato VALIDA (da compilare)
     * @pre category != null
     */
    public Proposal createDraft(Category category) {
        assert category != null : "Category must not be null";
        return new Proposal(category);
    }

    /**
     * Valida una bozza secondo le 3 regole della specifica.
     *
     * Regola 1: tutti i campi obbligatori devono essere compilati
     * Regola 2: Termine iscrizione > oggi
     * Regola 3: Data evento > Termine iscrizione + 2 giorni
     *
     * @param draft      la proposta da validare
     * @param baseFields lista dei campi base
     * @param commonFields lista dei campi comuni
     * @throws InvalidProposalException se almeno una regola non è soddisfatta
     * @pre draft != null
     * @post draft.getState() == ProposalState.VALIDA (già impostato alla creazione)
     */
    public void validate(Proposal draft,
                         List<BaseField> baseFields,
                         List<CommonField> commonFields)
            throws InvalidProposalException {

        assert draft != null;

        // ── Regola 1: campi obbligatori ──────────────────────────
        List<String> missing = new ArrayList<>();

        for (BaseField f : baseFields) {
            if (f.isMandatory()) {
                String val = draft.getFieldValues().get(f.getName());
                if (val == null || val.isBlank()) missing.add(f.getName());
            }
        }
        for (CommonField f : commonFields) {
            if (f.isMandatory()) {
                String val = draft.getFieldValues().get(f.getName());
                if (val == null || val.isBlank()) missing.add(f.getName());
            }
        }
        for (SpecificField f : draft.getCategory().getSpecificFields()) {
            if (f.isMandatory()) {
                String val = draft.getFieldValues().get(f.getName());
                if (val == null || val.isBlank()) missing.add(f.getName());
            }
        }

        if (!missing.isEmpty()) {
            throw new InvalidProposalException(
                "campi obbligatori mancanti: " + String.join(", ", missing));
        }

        // ── Regola 2: Termine iscrizione > oggi ──────────────────
        LocalDate deadline = parseDate(draft.getDeadlineStr(),
                "Termine ultimo di iscrizione");

        if (!deadline.isAfter(LocalDate.now())) {
            throw new InvalidProposalException(
                "il termine di iscrizione (" + fmt(deadline) +
                ") deve essere strettamente successivo alla data odierna (" +
                fmt(LocalDate.now()) + ").");
        }

        // ── Regola 3: Data evento > Termine + 2 giorni ───────────
        LocalDate eventDate = parseDate(draft.getEventDateStr(), "Data");

        if (!eventDate.isAfter(deadline.plusDays(2))) {
            throw new InvalidProposalException(
                "la data dell'evento (" + fmt(eventDate) +
                ") deve essere almeno 2 giorni dopo il termine di iscrizione (" +
                fmt(deadline) + "). Data minima consentita: " +
                fmt(deadline.plusDays(3)) + ".");
        }

        // Tutte le regole passate: la proposta rimane VALIDA
        assert draft.getState() == ProposalState.VALIDA;
    }

    // ================================================================
    // UC-12: PUBBLICAZIONE
    // ================================================================

    /**
     * Pubblica una proposta valida in bacheca.
     *
     * Ri-valida la proposta al momento della pubblicazione (le date
     * potrebbero essere scadute se la sessione è durata a lungo).
     * Se la ri-validazione passa, la proposta diventa APERTA,
     * viene aggiunta ad ApplicationState e potrà essere salvata su disco.
     *
     * @param proposal   la proposta da pubblicare (stato VALIDA)
     * @param baseFields lista dei campi base (per ri-validazione)
     * @param commonFields lista dei campi comuni (per ri-validazione)
     * @throws InvalidProposalException se la proposta non è più valida
     * @pre proposal != null && proposal.getState() == ProposalState.VALIDA
     * @post proposal.getState() == ProposalState.APERTA
     * @post state.getProposals() contiene la proposta
     */
    public void publish(Proposal proposal,
                        List<BaseField> baseFields,
                        List<CommonField> commonFields)
            throws InvalidProposalException {

        assert proposal != null;
        assert proposal.getState() == ProposalState.VALIDA :
            "Solo proposte VALIDE possono essere pubblicate";

        // Ri-valida (per sicurezza: le date potrebbero essere scadute)
        validate(proposal, baseFields, commonFields);

        // Pubblica
        proposal.setState(ProposalState.APERTA);
        proposal.setPublishDate(LocalDate.now());
        state.getProposalsMutable().add(proposal);

        assert proposal.getState() == ProposalState.APERTA;
        assert state.getProposals().contains(proposal);
    }

    // ================================================================
    // UC-13: LETTURA BACHECA
    // ================================================================

    /**
     * Restituisce le proposte aperte per una categoria specifica,
     * ordinate per data di pubblicazione (più recente prima).
     *
     * @param categoryName nome della categoria (null o blank = tutte)
     * @return lista di proposte con stato APERTA per la categoria richiesta
     */
    public List<Proposal> getBoardByCategory(String categoryName) {
        return state.getProposals().stream()
                .filter(p -> p.getState().isVisible())
                .filter(p -> categoryName == null || categoryName.isBlank()
                        || p.getCategory().getName().equalsIgnoreCase(categoryName))
                .sorted((a, b) -> {
                    if (a.getPublishDate() == null) return 1;
                    if (b.getPublishDate() == null) return -1;
                    return b.getPublishDate().compareTo(a.getPublishDate());
                })
                .collect(Collectors.toList());
    }

    /**
     * Restituisce tutte le proposte visibili in bacheca (tutte le categorie).
     */
    public List<Proposal> getAllOpenProposals() {
        return getBoardByCategory(null);
    }

    /**
     * Cerca una proposta per ID.
     *
     * @throws ProposalNotFoundException se non trovata
     */
    public Proposal getProposalById(String id) throws ProposalNotFoundException {
        assert id != null;
        return state.getProposals().stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new ProposalNotFoundException(id));
    }

    // ================================================================
    // UTILITY PRIVATA
    // ================================================================

    /**
     * Parsa una stringa data nel formato dd/MM/yyyy.
     * Lancia InvalidProposalException se il formato non è valido.
     */
    private LocalDate parseDate(String value, String fieldName)
            throws InvalidProposalException {
        if (value == null || value.isBlank()) {
            throw new InvalidProposalException(
                "il campo '" + fieldName + "' non contiene una data valida.");
        }
        try {
            return LocalDate.parse(value.trim(), DATE_FMT);
        } catch (DateTimeParseException e) {
            throw new InvalidProposalException(
                "il campo '" + fieldName + "' ha un formato data non valido: '" +
                value + "'. Formato atteso: gg/mm/aaaa");
        }
    }

    private String fmt(LocalDate d) {
        return d.format(DATE_FMT);
    }
}