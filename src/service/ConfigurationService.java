package service;

import exception.BaseFieldsAlreadyInitializedException;
import model.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ConfigurationService — gestisce l'inizializzazione una-tantum dei campi base.
 *
 * I campi base sono definiti dalla sezione GENERALITÀ della specifica:
 *   Titolo, Numero di partecipanti, Termine ultimo di iscrizione,
 *   Luogo, Data, Ora, Quota individuale, Data conclusiva.
 *
 * Vengono inizializzati automaticamente al primo avvio e mostrati
 * al configuratore per conferma. Una volta creati sono immutabili.
 *
 * Invariant: state != null
 */
public class ConfigurationService {

    private final ApplicationState state;
    
    /**
     * Campi base predefiniti dalla sezione GENERALITA' della specifica.
     * Ordine, nomi, tipi e obbligatorietà sono fissi e non modificabili.
     *
     * Ora, Quota individuale e Data conclusiva sono facoltativi perché
     * non tutte le iniziative richiedono questi dati.
     */
    public static final List<BaseField> PREDEFINED_BASE_FIELDS = Arrays.asList(
        new BaseField("Titolo",                       FieldType.STRING,   true),
        new BaseField("Numero di partecipanti",       FieldType.INTEGER,  true),
        new BaseField("Termine ultimo di iscrizione", FieldType.DATE,     true),
        new BaseField("Luogo",                        FieldType.STRING,   true),
        new BaseField("Data",                         FieldType.DATE,     true),
        new BaseField("Ora",                          FieldType.TIME,     false),
        new BaseField("Quota individuale",            FieldType.DECIMAL,  false),
        new BaseField("Data conclusiva",              FieldType.DATE,     false)
    );
    

    /** @pre state != null */
    public ConfigurationService(ApplicationState state) {
        assert state != null;
        this.state = state;
    }

    // ----------------------------------------------------------------
    // Queries
    // ----------------------------------------------------------------

    /** @return true se i campi base sono già stati inizializzati */
    public boolean areBaseFieldsInitialised() {
        return state.isBaseFieldsLocked();
    }

    /** @return lista immutabile dei campi base */
    public List<BaseField> getBaseFields() {
        return state.getBaseFields();
    }

    // ----------------------------------------------------------------
    // Commands
    // ----------------------------------------------------------------

    /**
     * Salva i campi base predefiniti e li blocca permanentemente.
     *
     * Chiamato dal Controller dopo che il configuratore ha confermato
     * la lista mostrata a schermo. I campi non vengono digitati
     * dall'utente: vengono proposti dal sistema e approvati.
     *
     * @throws BaseFieldsAlreadyInitializedException se gia' inizializzati
     * @pre !areBaseFieldsInitialised()
     * @post areBaseFieldsInitialised()
     * @post getBaseFields().size() == PREDEFINED_BASE_FIELDS.size()
     */
    public void initBaseFields(List<BaseField> fields) throws BaseFieldsAlreadyInitializedException {
        if (areBaseFieldsInitialised()) {
            throw new BaseFieldsAlreadyInitializedException();
        }
        
        // Controllo duplicati
        Set<String> names = new HashSet<>();
        for (BaseField f : fields) {
            String normalized = f.getName().trim().toLowerCase();
            if (!names.add(normalized)) {
                throw new IllegalArgumentException(
                    "Duplicato rilevato nel campo base: " + f.getName()
                );
            }
        }
        
        // Verifica che tutti i predefiniti siano presenti
        for (BaseField predefined : PREDEFINED_BASE_FIELDS) {
            boolean found = fields.stream()
                .anyMatch(f -> f.getName().equalsIgnoreCase(predefined.getName()));
            
            if (!found) {
                throw new IllegalArgumentException(
                    "Campo base obbligatorio mancante: " + predefined.getName()
                );
            }
        }

        state.getBaseFieldsMutable().addAll(fields);
        state.setBaseFieldsLocked(true);

        assert areBaseFieldsInitialised();
        assert state.isBaseFieldsLocked();
        assert getBaseFields().size() == fields.size();
    }
}