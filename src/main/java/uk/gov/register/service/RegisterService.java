package uk.gov.register.service;

import org.skife.jdbi.v2.DBI;
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
    private final DBI dbi;
    private final MemoizationStore memoizationStore;
    private final RegistersConfiguration registersConfiguration;

    @Inject
    public RegisterService(RegisterNameConfiguration registerNameConfig, DBI dbi, MemoizationStore memoizationStore, RegistersConfiguration registersConfiguration) {
        this.registerNameConfig = registerNameConfig;
        this.dbi = dbi;
        this.memoizationStore = memoizationStore;
        this.registersConfiguration = registersConfiguration;
    }

    public void asAtomicRegisterOperation(Consumer<Register> callback) {
        PostgresDriverTransactional.useTransaction(dbi, memoizationStore, registersConfiguration, postgresDriver -> {
            Register register = new PostgresRegister(registerNameConfig, postgresDriver);
            callback.accept(register);
        });
    }
}
