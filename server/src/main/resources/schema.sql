CREATE SCHEMA IF NOT EXISTS PRUEBA_MCP;
SET SCHEMA PRUEBA_MCP;

CREATE TABLE customer (
    id BIGINT PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    document_number VARCHAR(30) NOT NULL,
    email VARCHAR(200) NOT NULL,
    CONSTRAINT uq_customer_document_number UNIQUE (document_number),
    CONSTRAINT uq_customer_email UNIQUE (email)
);

CREATE TABLE branch (
    id BIGINT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    city VARCHAR(100) NOT NULL,
    address VARCHAR(200) NOT NULL,
    phone VARCHAR(30)
);

CREATE TABLE bank_account (
     id BIGINT PRIMARY KEY,
     customer_id BIGINT NOT NULL,
     branch_id BIGINT NOT NULL,
     account_number VARCHAR(40) NOT NULL,
     account_type VARCHAR(40) NOT NULL,
     balance DECIMAL(15,2) NOT NULL,
     opened_at DATE NOT NULL,
     CONSTRAINT uq_bank_account_account_number UNIQUE (account_number),
     CONSTRAINT fk_bank_account_customer FOREIGN KEY (customer_id) REFERENCES customer(id),
     CONSTRAINT fk_bank_account_branch FOREIGN KEY (branch_id) REFERENCES branch(id)
);

CREATE TABLE card (
      id BIGINT PRIMARY KEY,
      bank_account_id BIGINT NOT NULL,
      card_number VARCHAR(30) NOT NULL,
      card_type VARCHAR(20) NOT NULL,
      expiry_date DATE NOT NULL,
      CONSTRAINT uq_card_card_number UNIQUE (card_number),
      CONSTRAINT fk_card_bank_account FOREIGN KEY (bank_account_id) REFERENCES bank_account(id)
);

CREATE TABLE transaction_type (
     id BIGINT PRIMARY KEY,
     name VARCHAR(50) NOT NULL,
     description VARCHAR(200),
     is_incoming BOOLEAN NOT NULL,
     is_outgoing BOOLEAN NOT NULL,
     CONSTRAINT uq_transaction_type_name UNIQUE (name)
);

CREATE TABLE bank_transaction (
     id BIGINT PRIMARY KEY,
     bank_account_id BIGINT NOT NULL,
     transaction_type_id BIGINT NOT NULL,
     transaction_date TIMESTAMP NOT NULL,
     amount DECIMAL(15,2) NOT NULL,
     reference VARCHAR(80),
     CONSTRAINT fk_bank_transaction_account FOREIGN KEY (bank_account_id) REFERENCES bank_account(id),
     CONSTRAINT fk_bank_transaction_type FOREIGN KEY (transaction_type_id) REFERENCES transaction_type(id)
);

CREATE TABLE loan (
      id BIGINT PRIMARY KEY,
      customer_id BIGINT NOT NULL,
      branch_id BIGINT NOT NULL,
      amount DECIMAL(15,2) NOT NULL,
      status VARCHAR(30) NOT NULL,
      CONSTRAINT fk_loan_customer FOREIGN KEY (customer_id) REFERENCES customer(id),
      CONSTRAINT fk_loan_branch FOREIGN KEY (branch_id) REFERENCES branch(id)
);

CREATE TABLE loan_payment (
     id BIGINT PRIMARY KEY,
     loan_id BIGINT NOT NULL,
     payment_date DATE NOT NULL,
     amount DECIMAL(15,2) NOT NULL,
     payment_method VARCHAR(40) NOT NULL,
     CONSTRAINT fk_loan_payment_loan FOREIGN KEY (loan_id) REFERENCES loan(id)
);

-- Indices de campos mas consultados
CREATE INDEX idx_customer_document_number ON customer(document_number);
CREATE INDEX idx_customer_email ON customer(email);

CREATE INDEX idx_bank_account_account_number ON bank_account(account_number);
CREATE INDEX idx_bank_account_customer_id ON bank_account(customer_id);
CREATE INDEX idx_bank_account_branch_id ON bank_account(branch_id);

CREATE INDEX idx_card_card_number ON card(card_number);
CREATE INDEX idx_card_bank_account_id ON card(bank_account_id);

CREATE INDEX idx_bank_transaction_account_id ON bank_transaction(bank_account_id);
CREATE INDEX idx_bank_transaction_type_id ON bank_transaction(transaction_type_id);
CREATE INDEX idx_bank_transaction_date ON bank_transaction(transaction_date);

CREATE INDEX idx_loan_customer_id ON loan(customer_id);
CREATE INDEX idx_loan_branch_id ON loan(branch_id);

CREATE INDEX idx_loan_payment_loan_id ON loan_payment(loan_id);

