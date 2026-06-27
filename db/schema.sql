-- =====================================================================
-- Mini-ATS — Supabase schema (OPTION 2: Java/Spring + JPA friendly)
-- Same as ats_schema.sql but role/stage are text+CHECK instead of native
-- Postgres enums, so Hibernate maps them cleanly with @Enumerated(STRING).
-- Run in Supabase Dashboard → SQL Editor.
-- =====================================================================

create extension if not exists pg_trgm;

-- ---------------------------------------------------------------------
-- Profiles (1:1 with auth.users)
-- ---------------------------------------------------------------------
create table public.profiles (
  id          uuid primary key references auth.users (id) on delete cascade,
  email       text not null,
  full_name   text not null default '',
  role        text not null default 'customer'
              check (role in ('admin','customer')),
  created_at  timestamptz not null default now()
);

comment on table public.profiles is
  'Application profile + role for each Supabase Auth user.';

-- ---------------------------------------------------------------------
-- is_admin(): SECURITY DEFINER so it reads profiles without recursing
-- through the profiles RLS policy.
-- ---------------------------------------------------------------------
create or replace function public.is_admin()
returns boolean
language sql
security definer
set search_path = public
stable
as $$
  select exists (
    select 1 from public.profiles
    where id = auth.uid() and role = 'admin'
  );
$$;

-- ---------------------------------------------------------------------
-- Auto-create a profile when an auth user is created.
-- role + full_name can be passed in signup metadata; default role customer.
-- ---------------------------------------------------------------------
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.profiles (id, email, full_name, role)
  values (
    new.id,
    new.email,
    coalesce(new.raw_user_meta_data ->> 'full_name', ''),
    coalesce(new.raw_user_meta_data ->> 'role', 'customer')
  );
  return new;
end;
$$;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- ---------------------------------------------------------------------
-- Jobs
-- ---------------------------------------------------------------------
create table public.jobs (
  id          uuid primary key default gen_random_uuid(),
  owner_id    uuid not null references public.profiles (id) on delete cascade,
  title       text not null,
  description text,
  location    text,
  is_open     boolean not null default true,
  created_at  timestamptz not null default now()
);

create index jobs_owner_id_idx on public.jobs (owner_id);

-- ---------------------------------------------------------------------
-- Candidates
-- ---------------------------------------------------------------------
create table public.candidates (
  id            uuid primary key default gen_random_uuid(),
  owner_id      uuid not null references public.profiles (id) on delete cascade,
  job_id        uuid references public.jobs (id) on delete set null,
  full_name     text not null,
  email         text,
  linkedin_url  text,
  notes         text,
  stage         text not null default 'applied'
                check (stage in ('applied','screening','interview','offer','hired','rejected')),
  position      double precision not null default 0,
  cv_path       text,
  cv_filename   text,
  created_at    timestamptz not null default now()
);

create index candidates_owner_id_idx  on public.candidates (owner_id);
create index candidates_job_id_idx    on public.candidates (job_id);
create index candidates_stage_idx     on public.candidates (stage);
create index candidates_name_trgm_idx on public.candidates
  using gin (full_name gin_trgm_ops);

-- =====================================================================
-- Row Level Security (defense-in-depth; Spring also enforces authz)
-- =====================================================================
alter table public.profiles   enable row level security;
alter table public.jobs       enable row level security;
alter table public.candidates enable row level security;

-- profiles
create policy "profiles: read own or admin"
  on public.profiles for select
  using ( id = auth.uid() or public.is_admin() );
create policy "profiles: update own or admin"
  on public.profiles for update
  using ( id = auth.uid() or public.is_admin() )
  with check ( id = auth.uid() or public.is_admin() );
create policy "profiles: admin insert"
  on public.profiles for insert
  with check ( public.is_admin() );
create policy "profiles: admin delete"
  on public.profiles for delete
  using ( public.is_admin() );

-- jobs
create policy "jobs: read own or admin"
  on public.jobs for select
  using ( owner_id = auth.uid() or public.is_admin() );
create policy "jobs: insert own or admin"
  on public.jobs for insert
  with check ( owner_id = auth.uid() or public.is_admin() );
create policy "jobs: update own or admin"
  on public.jobs for update
  using ( owner_id = auth.uid() or public.is_admin() )
  with check ( owner_id = auth.uid() or public.is_admin() );
create policy "jobs: delete own or admin"
  on public.jobs for delete
  using ( owner_id = auth.uid() or public.is_admin() );

-- candidates
create policy "candidates: read own or admin"
  on public.candidates for select
  using ( owner_id = auth.uid() or public.is_admin() );
create policy "candidates: insert own or admin"
  on public.candidates for insert
  with check ( owner_id = auth.uid() or public.is_admin() );
create policy "candidates: update own or admin"
  on public.candidates for update
  using ( owner_id = auth.uid() or public.is_admin() )
  with check ( owner_id = auth.uid() or public.is_admin() );
create policy "candidates: delete own or admin"
  on public.candidates for delete
  using ( owner_id = auth.uid() or public.is_admin() );

-- =====================================================================
-- Bootstrap first admin (after creating the user in Auth → Users):
--   update public.profiles set role='admin' where email='you@example.com';
-- ==========================