-- ============================================================================
-- DSK VIDEO - SUPABASE POSTGRESQL SCHEMA & SECURITY POLICIES
-- Safe, idempotent migration (CREATE TABLE IF NOT EXISTS). Preserves existing data.
-- Run in Supabase SQL Editor for project: https://ottlzprymsdfgbgsrmxn.supabase.co
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. profiles
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

-- 2. user_roles
CREATE TABLE IF NOT EXISTS public.user_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    role TEXT NOT NULL CHECK (role IN ('user', 'admin')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, role)
);

-- Helper security-definer function for RLS
CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS BOOLEAN
LANGUAGE SQL
SECURITY DEFINER
STABLE
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.user_roles
        WHERE user_id = auth.uid() AND role = 'admin'
    );
$$;

-- 13. categories
CREATE TABLE IF NOT EXISTS public.categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    slug TEXT NOT NULL UNIQUE,
    section_type TEXT NOT NULL DEFAULT 'movie',
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 3. movies
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

-- 4. web_series
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

-- 5. seasons
CREATE TABLE IF NOT EXISTS public.seasons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    series_id UUID NOT NULL REFERENCES public.web_series(id) ON DELETE CASCADE,
    season_number INT NOT NULL,
    title TEXT NOT NULL DEFAULT '',
    description TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (series_id, season_number)
);

-- 6. episodes
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

-- 7. live_channels
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

-- 8. sports_content
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

-- 9. kids_content
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

-- 10. watch_history
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

-- 11. watch_progress
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

-- 12. watchlist
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

-- 14. admin_activity_logs
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

-- Indexes for fast OTT queries
CREATE INDEX IF NOT EXISTS idx_movies_published_created ON public.movies(is_published, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_movies_language_genre ON public.movies(language, genre);
CREATE INDEX IF NOT EXISTS idx_seasons_series ON public.seasons(series_id, season_number);
CREATE INDEX IF NOT EXISTS idx_episodes_season ON public.episodes(season_id, episode_number);
CREATE INDEX IF NOT EXISTS idx_watch_progress_user ON public.watch_progress(user_id, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_watch_history_user ON public.watch_history(user_id, watched_at DESC);
CREATE INDEX IF NOT EXISTS idx_watchlist_user ON public.watchlist(user_id, created_at DESC);

-- Enable Row Level Security (RLS)
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.movies ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.web_series ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.seasons ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.episodes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.live_channels ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.sports_content ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.kids_content ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.watch_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.watch_progress ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.watchlist ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.admin_activity_logs ENABLE ROW LEVEL SECURITY;

-- Policies (Idempotent DROP IF EXISTS + CREATE)
DROP POLICY IF EXISTS "Users read own profile or admin" ON public.profiles;
CREATE POLICY "Users read own profile or admin" ON public.profiles FOR SELECT USING (auth.uid() = id OR public.is_admin());
DROP POLICY IF EXISTS "Users insert own profile" ON public.profiles;
CREATE POLICY "Users insert own profile" ON public.profiles FOR INSERT WITH CHECK (auth.uid() = id);
DROP POLICY IF EXISTS "Users update own profile" ON public.profiles;
CREATE POLICY "Users update own profile" ON public.profiles FOR UPDATE USING (auth.uid() = id);

DROP POLICY IF EXISTS "Users read own roles or admin" ON public.user_roles;
CREATE POLICY "Users read own roles or admin" ON public.user_roles FOR SELECT USING (auth.uid() = user_id OR public.is_admin());
DROP POLICY IF EXISTS "Only admins manage roles" ON public.user_roles;
CREATE POLICY "Only admins manage roles" ON public.user_roles FOR ALL USING (public.is_admin());

DROP POLICY IF EXISTS "Authenticated read published movies" ON public.movies;
CREATE POLICY "Authenticated read published movies" ON public.movies FOR SELECT USING (is_published = TRUE OR public.is_admin());
DROP POLICY IF EXISTS "Only admins write movies" ON public.movies;
CREATE POLICY "Only admins write movies" ON public.movies FOR ALL USING (public.is_admin());

DROP POLICY IF EXISTS "Authenticated read published series" ON public.web_series;
CREATE POLICY "Authenticated read published series" ON public.web_series FOR SELECT USING (is_published = TRUE OR public.is_admin());
DROP POLICY IF EXISTS "Only admins write series" ON public.web_series;
CREATE POLICY "Only admins write series" ON public.web_series FOR ALL USING (public.is_admin());

DROP POLICY IF EXISTS "Authenticated read seasons" ON public.seasons;
CREATE POLICY "Authenticated read seasons" ON public.seasons FOR SELECT USING (auth.role() = 'authenticated' OR public.is_admin());
DROP POLICY IF EXISTS "Only admins write seasons" ON public.seasons;
CREATE POLICY "Only admins write seasons" ON public.seasons FOR ALL USING (public.is_admin());

DROP POLICY IF EXISTS "Authenticated read episodes" ON public.episodes;
CREATE POLICY "Authenticated read episodes" ON public.episodes FOR SELECT USING (auth.role() = 'authenticated' OR public.is_admin());
DROP POLICY IF EXISTS "Only admins write episodes" ON public.episodes;
CREATE POLICY "Only admins write episodes" ON public.episodes FOR ALL USING (public.is_admin());

DROP POLICY IF EXISTS "Authenticated read published channels" ON public.live_channels;
CREATE POLICY "Authenticated read published channels" ON public.live_channels FOR SELECT USING (is_published = TRUE OR public.is_admin());
DROP POLICY IF EXISTS "Only admins write channels" ON public.live_channels;
CREATE POLICY "Only admins write channels" ON public.live_channels FOR ALL USING (public.is_admin());

DROP POLICY IF EXISTS "Authenticated read published sports" ON public.sports_content;
CREATE POLICY "Authenticated read published sports" ON public.sports_content FOR SELECT USING (is_published = TRUE OR public.is_admin());
DROP POLICY IF EXISTS "Only admins write sports" ON public.sports_content;
CREATE POLICY "Only admins write sports" ON public.sports_content FOR ALL USING (public.is_admin());

DROP POLICY IF EXISTS "Authenticated read published kids" ON public.kids_content;
CREATE POLICY "Authenticated read published kids" ON public.kids_content FOR SELECT USING (is_published = TRUE OR public.is_admin());
DROP POLICY IF EXISTS "Only admins write kids" ON public.kids_content;
CREATE POLICY "Only admins write kids" ON public.kids_content FOR ALL USING (public.is_admin());

DROP POLICY IF EXISTS "Authenticated read categories" ON public.categories;
CREATE POLICY "Authenticated read categories" ON public.categories FOR SELECT USING (TRUE);
DROP POLICY IF EXISTS "Only admins write categories" ON public.categories;
CREATE POLICY "Only admins write categories" ON public.categories FOR ALL USING (public.is_admin());

DROP POLICY IF EXISTS "Users manage own watch_history" ON public.watch_history;
CREATE POLICY "Users manage own watch_history" ON public.watch_history FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users manage own watch_progress" ON public.watch_progress;
CREATE POLICY "Users manage own watch_progress" ON public.watch_progress FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users manage own watchlist" ON public.watchlist;
CREATE POLICY "Users manage own watchlist" ON public.watchlist FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Only admins read and write logs" ON public.admin_activity_logs;
CREATE POLICY "Only admins read and write logs" ON public.admin_activity_logs FOR ALL USING (public.is_admin()) WITH CHECK (public.is_admin());
