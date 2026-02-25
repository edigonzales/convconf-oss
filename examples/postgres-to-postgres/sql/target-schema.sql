CREATE TABLE tgt_organisation (id TEXT PRIMARY KEY, name TEXT, form_code INT);
CREATE TABLE tgt_person (id TEXT PRIMARY KEY, last_name TEXT, first_name TEXT, org_id TEXT);
CREATE TABLE tgt_event (id TEXT PRIMARY KEY, title TEXT, place TEXT, event_date TEXT);
CREATE TABLE tgt_comment (comment_id TEXT PRIMARY KEY, event_id TEXT, struct_code INT, type_code INT, text_value TEXT, by_org TEXT);
