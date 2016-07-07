package flyway.oskari;

import fi.nls.oskari.db.BundleHelper;
import fi.nls.oskari.domain.map.view.Bundle;
import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by SMAKINEN on 25.8.2015.
 */
public class V1_34_0_5__register_admin_registers_bundle implements JdbcMigration {

    private static final String NAMESPACE = "nba";
    private static final String BUNDLE_ID = "admin-registers";

    public void migrate(Connection connection) throws SQLException {
        // BundleHelper checks if these bundles are already registered
        Bundle bundle = new Bundle();
        bundle.setName(BUNDLE_ID);
        bundle.setStartup(BundleHelper.getDefaultBundleStartup(NAMESPACE, BUNDLE_ID, "admin-registers"));
        BundleHelper.registerBundle(bundle, connection);

    }
}
