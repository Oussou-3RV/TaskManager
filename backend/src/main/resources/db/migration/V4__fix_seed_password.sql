-- Le hash de V2__seed.sql ne correspondait pas au mot de passe documente
-- "password123" pour les comptes de demonstration. On corrige ici plutot
-- que de modifier V2 (deja appliquee), pour ne pas casser la validation
-- des checksums Flyway sur les bases existantes.
UPDATE users
SET password_hash = '$2b$10$9bmIsVT79fh8aho7Q6BOhe0O4.jcTmwaxPajq/6JhCAtdK.xeAMfK'
WHERE username IN ('admin', 'operateur', 'auditeur');
