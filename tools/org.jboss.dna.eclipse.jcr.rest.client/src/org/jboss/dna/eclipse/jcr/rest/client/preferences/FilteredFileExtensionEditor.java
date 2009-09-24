package org.jboss.dna.eclipse.jcr.rest.client.preferences;

import static org.jboss.dna.eclipse.jcr.rest.client.IUiConstants.FILTERED_FILE_EXTENSIONS_PREFERENCE;
import static org.jboss.dna.eclipse.jcr.rest.client.RestClientI18n.dnaPreferencePageFilteredFileExtensionsLabel;
import static org.jboss.dna.eclipse.jcr.rest.client.RestClientI18n.newFilteredFileExtensionDialogLabel;
import static org.jboss.dna.eclipse.jcr.rest.client.RestClientI18n.newFilteredFileExtensionDialogTitle;
import static org.jboss.dna.eclipse.jcr.rest.client.preferences.PrefUtils.FILE_EXT_DELIMITER;
import static org.jboss.dna.eclipse.jcr.rest.client.preferences.PrefUtils.FILE_EXT_INVALID_CHARS;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import org.eclipse.jface.preference.ListEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.widgets.Composite;
import org.jboss.dna.eclipse.jcr.rest.client.Utils;

/**
 * The <code>FilteredFileExtensionEditor</code> is an editor for managing a set of filtered file extensions.
 */
public final class FilteredFileExtensionEditor extends ListEditor implements VerifyListener {

    // =======================================================================================================================
    // Fields
    // =======================================================================================================================

    /**
     * The current set of file extensions.
     */
    private final Set<String> extensions;

    // =======================================================================================================================
    // Constructors
    // =======================================================================================================================

    /**
     * @param parent the parent control
     */
    public FilteredFileExtensionEditor( Composite parent ) {
        super(FILTERED_FILE_EXTENSIONS_PREFERENCE, dnaPreferencePageFilteredFileExtensionsLabel.text(), parent);
        this.extensions = new TreeSet<String>();
    }

    // =======================================================================================================================
    // Methods
    // =======================================================================================================================

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.preference.ListEditor#createList(java.lang.String[])
     */
    @Override
    protected String createList( String[] items ) {
        return Utils.combineTokens(items, FILE_EXT_DELIMITER);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.preference.ListEditor#getNewInputObject()
     */
    @Override
    protected String getNewInputObject() {
        NewItemDialog dialog = new NewItemDialog(getShell(), newFilteredFileExtensionDialogTitle.text(),
                                                 newFilteredFileExtensionDialogLabel.text(), this);

        if (dialog.open() == Window.OK) {
            String extension = dialog.getNewItem();

            // add new extension
            if (extension != null) {
                this.extensions.add(extension);
                return extension;
            }
        }

        // user canceled dialog
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.preference.ListEditor#parseString(java.lang.String)
     */
    @Override
    protected String[] parseString( String stringList ) {
        String[] values = Utils.getTokens(stringList, Character.toString(FILE_EXT_DELIMITER), true);

        this.extensions.clear();
        this.extensions.addAll(Arrays.asList(values));

        return values;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.swt.events.VerifyListener#verifyText(org.eclipse.swt.events.VerifyEvent)
     */
    @Override
    public void verifyText( VerifyEvent event ) {
        for (char c : FILE_EXT_INVALID_CHARS.toCharArray()) {
            if (c == event.character) {
                event.doit = false;
                break;
            }
        }
    }

}
