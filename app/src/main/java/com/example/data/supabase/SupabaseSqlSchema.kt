package com.example.data.supabase

object SupabaseSqlSchema {
    val MIGRATION_SQL = """
-- DSK VIDEO - 14-Table Idempotent PostgreSQL Migration
-- Preserves existing data & enforces zero-trust Row Level Security (RLS)
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT NOT NULL,
    username TEXT NOT NULL DEFAULT '',
    avatar_url TEXT NOT NULL DEFAULT '',
    preferred_language TEXT NOT NULL DEFAULT 'English',
    pin_hash TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.user_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    role TEXT NOT NULL CHECK (role IN ('user', 'admin')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, role)
);

CREATE TABLE IF NOT EXISTS public.admin_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES auth.users(id) ON DELETE CASCADE,
    full_name TEXT NOT NULL DEFAULT '',
    email TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'admin' CHECK (role IN ('admin', 'super_admin')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.admin_authorized_emails (
    email TEXT PRIMARY KEY,
    role TEXT NOT NULL DEFAULT 'admin' CHECK (role IN ('admin', 'super_admin')),
    authorized_by UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS BOOLEAN LANGUAGE SQL SECURITY DEFINER SET search_path = public STABLE AS $$
    SELECT EXISTS (SELECT 1 FROM public.admin_roles WHERE user_id = auth.uid() AND is_active = TRUE)
        OR EXISTS (SELECT 1 FROM public.user_roles WHERE user_id = auth.uid() AND role = 'admin');
$$;

CREATE OR REPLACE FUNCTION public.register_admin_account(p_full_name TEXT)
RETURNS BOOLEAN LANGUAGE PLPGSQL SECURITY DEFINER SET search_path = public AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_email TEXT;
    v_has_existing_admin BOOLEAN;
    v_authorized_role TEXT;
    v_assigned_role TEXT;
    v_clean_name TEXT := COALESCE(NULLIF(TRIM(p_full_name), ''), 'DSK Admin');
BEGIN
    IF v_uid IS NULL THEN RAISE EXCEPTION 'Authentication required.'; END IF;
    SELECT email INTO v_email FROM auth.users WHERE id = v_uid;
    IF v_email IS NULL THEN RAISE EXCEPTION 'User record not found.'; END IF;

    SELECT EXISTS (SELECT 1 FROM public.admin_roles WHERE is_active = TRUE) INTO v_has_existing_admin;
    IF NOT v_has_existing_admin THEN
        v_assigned_role := 'super_admin';
    ELSE
        SELECT role INTO v_authorized_role FROM public.admin_authorized_emails WHERE LOWER(email) = LOWER(v_email) LIMIT 1;
        IF v_authorized_role IS NULL THEN
            IF EXISTS (SELECT 1 FROM public.admin_roles WHERE user_id = v_uid AND is_active = TRUE) THEN
                RETURN TRUE;
            END IF;
            RAISE EXCEPTION 'Unauthorized: An active Super Admin already exists. Ask an existing Admin to authorize % in Admin Studio.', v_email;
        END IF;
        v_assigned_role := v_authorized_role;
    END IF;

    INSERT INTO public.admin_roles (user_id, full_name, email, role, is_active)
    VALUES (v_uid, v_clean_name, v_email, v_assigned_role, TRUE)
    ON CONFLICT (user_id) DO UPDATE SET full_name = EXCLUDED.full_name, email = EXCLUDED.email, is_active = TRUE;

    INSERT INTO public.user_roles (user_id, role) VALUES (v_uid, 'admin') ON CONFLICT (user_id, role) DO NOTHING;

    INSERT INTO public.admin_activity_logs (admin_id, admin_email, action, entity_type, entity_id, details)
    VALUES (v_uid, v_email, 'ADMIN_REGISTER', 'admin_roles', v_uid::TEXT, 'Server-verified admin registration (' || v_assigned_role || ')');
    RETURN TRUE;
END;
$$;

REVOKE ALL ON FUNCTION public.register_admin_account(TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.register_admin_account(TEXT) TO authenticated;

CREATE TABLE IF NOT EXISTS public.categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    slug TEXT NOT NULL UNIQUE,
    section_type TEXT NOT NULL DEFAULT 'movie',
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.movies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    poster_url TEXT NOT NULL DEFAULT '',
    banner_url TEXT NOT NULL DEFAULT '',
    release_year INT NOT NULL DEFAULT 2026,
    language TEXT NOT NULL DEFAULT 'Hindi',
    genre TEXT NOT NULL DEFAULT 'Drama',
    category TEXT NOT NULL DEFAULT 'Bollywood',
    duration_minutes INT NOT NULL DEFAULT 120,
    imdb_rating DOUBLE PRECISION NOT NULL DEFAULT 8.0,
    cast_members TEXT NOT NULL DEFAULT '',
    director TEXT NOT NULL DEFAULT '',
    trailer_url TEXT NOT NULL DEFAULT '',
    video_url TEXT NOT NULL,
    subtitle_url TEXT NOT NULL DEFAULT '',
    is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    is_trending BOOLEAN NOT NULL DEFAULT FALSE,
    is_popular BOOLEAN NOT NULL DEFAULT FALSE,
    is_latest BOOLEAN NOT NULL DEFAULT TRUE,
    is_published BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.web_series (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    poster_url TEXT NOT NULL DEFAULT '',
    banner_url TEXT NOT NULL DEFAULT '',
    release_year INT NOT NULL DEFAULT 2026,
    language TEXT NOT NULL DEFAULT 'Hindi',
    genre TEXT NOT NULL DEFAULT 'Thriller',
    imdb_rating DOUBLE PRECISION NOT NULL DEFAULT 8.2,
    cast_members TEXT NOT NULL DEFAULT '',
    director TEXT NOT NULL DEFAULT '',
    trailer_url TEXT NOT NULL DEFAULT '',
    is_popular BOOLEAN NOT NULL DEFAULT TRUE,
    is_published BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.seasons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    series_id UUID NOT NULL REFERENCES public.web_series(id) ON DELETE CASCADE,
    season_number INT NOT NULL,
    title TEXT NOT NULL DEFAULT '',
    description TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (series_id, season_number)
);

CREATE TABLE IF NOT EXISTS public.episodes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    series_id UUID NOT NULL REFERENCES public.web_series(id) ON DELETE CASCADE,
    season_id UUID NOT NULL REFERENCES public.seasons(id) ON DELETE CASCADE,
    episode_number INT NOT NULL,
    title TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    thumbnail_url TEXT NOT NULL DEFAULT '',
    duration_minutes INT NOT NULL DEFAULT 45,
    video_url TEXT NOT NULL,
    subtitle_url TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (season_id, episode_number)
);

CREATE TABLE IF NOT EXISTS public.live_channels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    logo_url TEXT NOT NULL DEFAULT '',
    category TEXT NOT NULL DEFAULT 'Entertainment',
    stream_url TEXT NOT NULL,
    language TEXT NOT NULL DEFAULT 'Hindi',
    is_live BOOLEAN NOT NULL DEFAULT TRUE,
    is_published BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.sports_content (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    thumbnail_url TEXT NOT NULL DEFAULT '',
    banner_url TEXT NOT NULL DEFAULT '',
    category TEXT NOT NULL DEFAULT 'Highlights',
    sport_type TEXT NOT NULL DEFAULT 'Cricket',
    duration_minutes INT NOT NULL DEFAULT 15,
    video_url TEXT NOT NULL,
    subtitle_url TEXT NOT NULL DEFAULT '',
    is_live BOOLEAN NOT NULL DEFAULT FALSE,
    is_published BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.kids_content (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    poster_url TEXT NOT NULL DEFAULT '',
    banner_url TEXT NOT NULL DEFAULT '',
    category TEXT NOT NULL DEFAULT 'Cartoons',
    age_rating TEXT NOT NULL DEFAULT 'U',
    language TEXT NOT NULL DEFAULT 'English',
    duration_minutes INT NOT NULL DEFAULT 22,
    video_url TEXT NOT NULL,
    subtitle_url TEXT NOT NULL DEFAULT '',
    is_published BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.watch_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    content_id TEXT NOT NULL,
    content_type TEXT NOT NULL,
    title TEXT NOT NULL,
    poster_url TEXT NOT NULL DEFAULT '',
    video_url TEXT NOT NULL DEFAULT '',
    watched_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, content_id)
);

CREATE TABLE IF NOT EXISTS public.watch_progress (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    content_id TEXT NOT NULL,
    content_type TEXT NOT NULL,
    title TEXT NOT NULL,
    poster_url TEXT NOT NULL DEFAULT '',
    video_url TEXT NOT NULL DEFAULT '',
    subtitle_url TEXT NOT NULL DEFAULT '',
    position_ms BIGINT NOT NULL DEFAULT 0,
    duration_ms BIGINT NOT NULL DEFAULT 0,
    episode_id TEXT NOT NULL DEFAULT '',
    season_id TEXT NOT NULL DEFAULT '',
    series_id TEXT NOT NULL DEFAULT '',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, content_id)
);

CREATE TABLE IF NOT EXISTS public.watchlist (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    content_id TEXT NOT NULL,
    content_type TEXT NOT NULL,
    title TEXT NOT NULL,
    poster_url TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, content_id)
);

CREATE TABLE IF NOT EXISTS public.admin_activity_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    admin_email TEXT NOT NULL DEFAULT '',
    action TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    entity_id TEXT NOT NULL DEFAULT '',
    details TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
""".trimIndent()
}
