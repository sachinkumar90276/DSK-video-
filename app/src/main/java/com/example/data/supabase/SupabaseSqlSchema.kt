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

CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS BOOLEAN LANGUAGE SQL SECURITY DEFINER STABLE AS $$
    SELECT EXISTS (SELECT 1 FROM public.user_roles WHERE user_id = auth.uid() AND role = 'admin');
$$;

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
