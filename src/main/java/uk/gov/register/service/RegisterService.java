package uk.gov.register.service;

import org.skife.jdbi.v2.DBI;
import uk.gov.register.configuration.RegisterFieldsConfiguration;
import uk.gov.register.configuration.RegisterNameConfiguration;
import uk.gov.register.configuration.RegistersConfiguration;
import uk.gov.register.core.PostgresRegister;
import uk.gov.register.core.Register;
import uk.gov.register.store.postgres.PostgresDriverTransactional;
import uk.gov.verifiablelog.store.memoization.MemoizationStore;

import javax.inject.Inject;
import java.util.function.Consumer;

public class RegisterService {
    private final RegisterNameConfiguration registerNameConfig;
    private final RegisterFieldsConfiguration registerFieldsConfiguration;
    private final DBI dbi;
    private final MemoizationStore memoizationStore;

    @Inject
    public RegisterService(RegisterNameConfiguration registerNameConfig, RegisterFieldsConfiguration registerFieldsConfiguration, DBI dbi, MemoizationStore memoizationStore, RegistersConfiguration registersConfiguration) {
        this.registerNameConfig = registerNameConfig;
        this.registerFieldsConfiguration = registerFieldsConfiguration;
        this.dbi = dbi;
        this.memoizationStore = memoizationStore;
    }

    public void asAtomicRegisterOperation(Consumer<Register> callback) {
        PostgresDriverTransactional.useTransaction(dbi, memoizationStore, postgresDriver -> {
            Register register = new PostgresRegister(registerNameConfig, registerFieldsConfiguration, postgresDriver);
            callback.accept(register);
        });
    }
}
