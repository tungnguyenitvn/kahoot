CREATE TABLE app_user (
 id uuid PRIMARY KEY, username varchar(120) NOT NULL UNIQUE,
 display_name varchar(80) NOT NULL, password_hash varchar(255) NOT NULL
);
CREATE TABLE quiz (
 id uuid PRIMARY KEY, owner_id uuid NOT NULL REFERENCES app_user(id),
 title varchar(120) NOT NULL, status varchar(16) NOT NULL CHECK(status IN ('DRAFT','PUBLISHED')),
 content jsonb NOT NULL, created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE game_room (
 id uuid PRIMARY KEY, quiz_id uuid NOT NULL REFERENCES quiz(id), owner_id uuid NOT NULL REFERENCES app_user(id),
 pin varchar(6) NOT NULL, title varchar(120) NOT NULL, content jsonb NOT NULL,
 phase varchar(16) NOT NULL DEFAULT 'PROVISIONING', archived_version bigint NOT NULL DEFAULT 0,
 created_at timestamptz NOT NULL DEFAULT now(), finished_at timestamptz
);
CREATE TABLE game_event (
 room_id uuid NOT NULL REFERENCES game_room(id), version bigint NOT NULL,
 stream_id varchar(80) NOT NULL, kind varchar(30) NOT NULL, payload jsonb NOT NULL,
 PRIMARY KEY(room_id,version), UNIQUE(room_id,stream_id)
);
CREATE TABLE participant (
 room_id uuid NOT NULL REFERENCES game_room(id), id uuid NOT NULL,
 name varchar(24) NOT NULL, principal_id varchar(80) NOT NULL, active boolean NOT NULL DEFAULT true,
 PRIMARY KEY(room_id,id), UNIQUE(room_id,principal_id)
);
CREATE TABLE answer (
 room_id uuid NOT NULL REFERENCES game_room(id), round_id uuid NOT NULL, participant_id uuid NOT NULL,
 selected_option integer NOT NULL, correct_order integer NOT NULL, points integer NOT NULL,
 accepted_at bigint NOT NULL, PRIMARY KEY(room_id,round_id,participant_id),
 FOREIGN KEY(room_id,participant_id) REFERENCES participant(room_id,id)
);
CREATE INDEX game_room_owner_idx ON game_room(owner_id,created_at DESC);
