package model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Rappresenta una proposta di iniziativa ricreativa.
 *
 * Ciclo di vita V2:
 *   [bozza in memoria] → VALIDA → APERTA (su disco)
 *
 * I valori dei campi sono memorizzati in una Map<String, String>
 * dove la chiave è il nome del campo e il valore è sempre una stringa.
 * La conversione al tipo corretto avviene nel ProposalService al momento
 * della validazione.
 *
 * La lista registrations è vuota in V2; in V3 conterrà gli username
 * dei fruitori iscritti.
 *
 * Invarianti:
 *   - id != null
 *   - category != null
 *   - state != null
 *   - fieldValues != null
 */
public class Proposal implements Serializable {

    private static final long serialVersionUID = 1L;

    // ---- identificazione ----
    private final String id;
    private final Category category;

    // ---- valori campi compilati (nome → valore stringa) ----
    private final Map<String, String> fieldValues;

    // ---- stato e date ----
    private ProposalState state;
    private LocalDate publishDate;

    // ---- iscrizioni (usate dalla V3) ----
    private final List<String> registrations; // lista di username

    /**
     * Costruttore usato da ProposalService.createDraft().
     *
     * @pre category != null
     */
    public Proposal(Category category) {
        assert category != null : "Category must not be null";
        this.id            = UUID.randomUUID().toString();
        this.category      = category;
        this.fieldValues   = new LinkedHashMap<>(); // ordine di inserimento
        this.state         = ProposalState.VALIDA;
        this.publishDate   = null;
        this.registrations = new ArrayList<>();
    }

    // ----------------------------------------------------------------
    // Getters
    // ----------------------------------------------------------------

    public String getId()           { return id; }
    public Category getCategory()   { return category; }
    public ProposalState getState() { return state; }
    public LocalDate getPublishDate() { return publishDate; }

    /** @return copia immutabile dei valori dei campi */
    public Map<String, String> getFieldValues() {
        return Collections.unmodifiableMap(fieldValues);
    }

    /** @return lista iscrizioni (vuota in V2) */
    public List<String> getRegistrations() {
        return Collections.unmodifiableList(registrations);
    }

    // ---- shortcut per i campi base più usati ----

    public String getTitle() {
        return fieldValues.getOrDefault("Titolo", "");
    }

    public String getEventDateStr() {
        return fieldValues.getOrDefault("Data", "");
    }

    public String getDeadlineStr() {
        return fieldValues.getOrDefault("Termine ultimo di iscrizione", "");
    }

    public String getPlace() {
        return fieldValues.getOrDefault("Luogo", "");
    }

    public String getQuota() {
        return fieldValues.getOrDefault("Quota individuale", "");
    }

    public String getParticipantsStr() {
        return fieldValues.getOrDefault("Numero di partecipanti", "");
    }

    // ----------------------------------------------------------------
    // Setters — usati solo da ProposalService
    // ----------------------------------------------------------------

    /**
     * Imposta o sovrascrive il valore di un campo.
     * @pre name != null
     */
    public void setFieldValue(String name, String value) {
        assert name != null : "Field name must not be null";
        fieldValues.put(name, value == null ? "" : value);
    }

    /**
     * @pre state != null
     * @pre !this.state.isTerminal()  (non si può cambiare uno stato terminale)
     */
    public void setState(ProposalState state) {
        assert state != null;
        this.state = state;
    }

    /**
     * @pre date != null
     */
    public void setPublishDate(LocalDate date) {
        assert date != null;
        this.publishDate = date;
    }

    // ----------------------------------------------------------------
    // Invariant
    // ----------------------------------------------------------------

    public boolean repOk() {
        return id != null && !id.isBlank()
            && category != null
            && state != null
            && fieldValues != null
            && registrations != null;
    }

    @Override
    public String toString() {
        return String.format("Proposal[%s | cat=%s | stato=%s | titolo=%s]",
            id.substring(0, 8), category.getName(), state, getTitle());
    }
}