#!/usr/bin/env python3
from __future__ import annotations

import json
import re
import sys
from collections import defaultdict
from pathlib import Path
from typing import Any

try:
    import tomllib
except ModuleNotFoundError:
    tomllib = None


MAX_SCAN_FILE_SIZE = 1024 * 1024  # 1MB
TEXT_EXTENSIONS = {
    ".py", ".js", ".ts", ".jsx", ".tsx", ".php", ".rb", ".go",
    ".java", ".kt", ".rs", ".yml", ".yaml", ".json", ".toml",
    ".ini", ".conf", ".txt", ".env", ".sh", ".html"
}


def safe_read_text(path: Path) -> str:
    try:
        if path.stat().st_size > MAX_SCAN_FILE_SIZE:
            return ""
        return path.read_text(encoding="utf-8", errors="ignore")
    except Exception:
        return ""


def add_evidence(evidence: dict[str, list[str]], source: str, value: str) -> None:
    if value not in evidence[source]:
        evidence[source].append(value)


def add_score(
    scores: dict[str, dict[str, int]],
    category: str,
    key: str,
    points: int
) -> None:
    scores[category][key] += points


def find_files(root: Path) -> list[Path]:
    files: list[Path] = []
    for path in root.rglob("*"):
        if path.is_file():
            files.append(path)
    return files


def parse_requirements(text: str) -> list[str]:
    pkgs = []
    for line in text.splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        pkg = re.split(r"[<>=~!;\[]", line, maxsplit=1)[0].strip()
        if pkg:
            pkgs.append(pkg.lower())
    return pkgs


def parse_package_json(text: str) -> dict[str, Any]:
    try:
        return json.loads(text)
    except Exception:
        return {}


def parse_composer_json(text: str) -> dict[str, Any]:
    try:
        return json.loads(text)
    except Exception:
        return {}


def parse_pyproject(text: str) -> dict[str, Any]:
    if tomllib is None:
        return {}
    try:
        return tomllib.loads(text)
    except Exception:
        return {}


def detect_from_dependency_names(
    deps: set[str],
    scores: dict[str, dict[str, int]],
    evidence: dict[str, list[str]],
    source_name: str
) -> None:
    dependency_map = {
        "language": {
            "flask": "python",
            "django": "python",
            "fastapi": "python",
            "jinja2": "python",
            "express": "nodejs",
            "next": "nodejs",
            "nestjs": "nodejs",
            "koa": "nodejs",
            "hapi": "nodejs",
            "laravel/framework": "php",
            "symfony/symfony": "php",
            "slim/slim": "php",
            "rails": "ruby",
            "sinatra": "ruby",
        },
        "framework": {
            "flask": "flask",
            "django": "django",
            "fastapi": "fastapi",
            "starlette": "starlette",
            "express": "express",
            "next": "nextjs",
            "nestjs": "nestjs",
            "koa": "koa",
            "hapi": "hapi",
            "laravel/framework": "laravel",
            "symfony/symfony": "symfony",
            "slim/slim": "slim",
            "rails": "rails",
            "sinatra": "sinatra",
        },
        "template_engine": {
            "jinja2": "jinja2",
            "ejs": "ejs",
            "pug": "pug",
            "nunjucks": "nunjucks",
            "twig/twig": "twig",
            "handlebars": "handlebars",
            "mustache": "mustache",
        },
        "database": {
            "psycopg2": "postgresql",
            "psycopg": "postgresql",
            "asyncpg": "postgresql",
            "pg": "postgresql",
            "mysqlclient": "mysql",
            "pymysql": "mysql",
            "mysql2": "mysql",
            "sqlite3": "sqlite",
            "redis": "redis",
            "ioredis": "redis",
            "mongoose": "mongodb",
            "mongodb": "mongodb",
            "sequelize": "sql-orm",
            "sqlalchemy": "sql-orm",
            "typeorm": "sql-orm",
            "prisma": "sql-orm",
        },
        "notable_libs": {
            "flask-login": "flask-login",
            "flask-session": "flask-session",
            "flask-jwt-extended": "flask-jwt-extended",
            "pyjwt": "pyjwt",
            "jsonwebtoken": "jsonwebtoken",
            "requests": "requests",
            "httpx": "httpx",
            "axios": "axios",
            "puppeteer": "puppeteer",
            "playwright": "playwright",
            "selenium": "selenium",
            "redis": "redis",
            "ioredis": "ioredis",
            "bcrypt": "bcrypt",
            "argon2-cffi": "argon2-cffi",
        }
    }

    for category, mapping in dependency_map.items():
        for dep_name, detected_value in mapping.items():
            if dep_name in deps:
                add_score(scores, category, detected_value, 3)
                add_evidence(evidence, source_name, f"dependency:{dep_name}")


def detect_risk_indicators_from_text(
    text: str,
    path: Path,
    result: dict[str, Any]
) -> None:
    source = str(path)

    patterns = [
        (r"render_template_string\s*\(", "server-side template rendering"),
        (r"jinja2\.Template\s*\(", "server-side template rendering"),
        (r"requests\.(get|post|request)\s*\(", "outbound http client"),
        (r"httpx\.(get|post|request)\s*\(", "outbound http client"),
        (r"axios\.(get|post|request)\s*\(", "outbound http client"),
        (r"\bfetch\s*\(", "outbound http client"),
        (r"os\.system\s*\(", "command execution"),
        (r"subprocess\.(run|Popen|call)\s*\(", "command execution"),
        (r"child_process\.(exec|execSync|spawn)\s*\(", "command execution"),
        (r"pickle\.loads\s*\(", "unsafe deserialization"),
        (r"yaml\.load\s*\(", "unsafe deserialization"),
        (r"jwt\.decode\s*\(", "jwt processing"),
        (r"jsonwebtoken", "jwt processing"),
        (r"puppeteer", "headless browser / admin bot"),
        (r"playwright", "headless browser / admin bot"),
        (r"selenium", "headless browser / admin bot"),
        (r"sqlite3", "database access"),
        (r"SELECT\s+.*\+|INSERT\s+.*\+|UPDATE\s+.*\+", "possible raw sql concatenation"),
    ]

    for pattern, label in patterns:
        if re.search(pattern, text, flags=re.IGNORECASE | re.MULTILINE):
            if label not in result["risk_indicators"]:
                result["risk_indicators"].append(label)
            add_evidence(result["evidence"], source, f"pattern:{label}")


def scan_filename_hints(
    path: Path,
    scores: dict[str, dict[str, int]],
    evidence: dict[str, list[str]]
) -> None:
    name = path.name.lower()

    if name == "requirements.txt" or name == "pyproject.toml":
        add_score(scores, "language", "python", 2)
        add_evidence(evidence, str(path), "filename:python-dependency-file")

    if name == "package.json":
        add_score(scores, "language", "nodejs", 2)
        add_evidence(evidence, str(path), "filename:node-dependency-file")

    if name == "composer.json":
        add_score(scores, "language", "php", 2)
        add_evidence(evidence, str(path), "filename:php-dependency-file")

    if name == "gemfile":
        add_score(scores, "language", "ruby", 2)
        add_evidence(evidence, str(path), "filename:ruby-dependency-file")

    if name == "go.mod":
        add_score(scores, "language", "go", 2)
        add_evidence(evidence, str(path), "filename:go-module-file")

    if name == "dockerfile":
        add_evidence(evidence, str(path), "filename:dockerfile")

    if "nginx" in name and path.suffix in {".conf", ""}:
        add_score(scores, "web_server", "nginx", 3)
        add_evidence(evidence, str(path), "filename:nginx-config")


def scan_source_patterns(
    text: str,
    path: Path,
    scores: dict[str, dict[str, int]],
    evidence: dict[str, list[str]]
) -> None:
    source = str(path)

    source_patterns = [
        ("language", "python", r"\bimport flask\b|\bfrom flask import\b|\bdef\s+\w+\("),
        ("framework", "flask", r"\bfrom flask import\b|\bFlask\s*\("),
        ("framework", "django", r"\bfrom django\b|\burlpatterns\b"),
        ("framework", "fastapi", r"\bfrom fastapi import\b|\bFastAPI\s*\("),
        ("framework", "express", r"\bexpress\s*=\s*require\(['\"]express['\"]\)|\bconst express = require"),
        ("framework", "nextjs", r"\bfrom ['\"]next['\"]|\bnext/config\b"),
        ("framework", "laravel", r"\bRoute::get\b|\bRoute::post\b|\bartisan\b"),
        ("framework", "rails", r"\bRails\.application\.routes\b|\bActionController::Base\b"),
        ("template_engine", "jinja2", r"\brender_template\b|\brender_template_string\b|\bjinja2\b"),
        ("template_engine", "ejs", r"\bres\.render\s*\(.*['\"].*ejs"),
        ("template_engine", "pug", r"\bres\.render\s*\(.*['\"].*pug"),
        ("database", "postgresql", r"\bpsycopg2\b|\bpostgres\b|\basyncpg\b"),
        ("database", "mysql", r"\bpymysql\b|\bmysql\b"),
        ("database", "sqlite", r"\bsqlite3\b"),
        ("database", "redis", r"\bredis\b|\bioredis\b"),
        ("web_server", "gunicorn", r"\bgunicorn\b"),
        ("web_server", "nginx", r"\bnginx\b"),
        ("notable_libs", "requests", r"\bimport requests\b|\brequests\."),
        ("notable_libs", "httpx", r"\bimport httpx\b|\bhttpx\."),
        ("notable_libs", "axios", r"\baxios\b"),
        ("notable_libs", "puppeteer", r"\bpuppeteer\b"),
        ("notable_libs", "playwright", r"\bplaywright\b"),
        ("notable_libs", "selenium", r"\bselenium\b"),
        ("notable_libs", "flask-login", r"\bflask_login\b|\bLoginManager\b"),
        ("notable_libs", "pyjwt", r"\bimport jwt\b|\bjwt\.decode\b"),
    ]

    for category, key, pattern in source_patterns:
        if re.search(pattern, text, flags=re.IGNORECASE | re.MULTILINE):
            add_score(scores, category, key, 4)
            add_evidence(evidence, source, f"pattern:{key}")


def choose_best(scores: dict[str, int]) -> tuple[str | None, int]:
    if not scores:
        return None, 0
    items = sorted(scores.items(), key=lambda x: x[1], reverse=True)
    return items[0]


def collect_dependencies(files: list[Path], evidence: dict[str, list[str]]) -> set[str]:
    deps: set[str] = set()

    for path in files:
        name = path.name.lower()
        text = safe_read_text(path)
        if not text:
            continue

        if name == "requirements.txt":
            parsed = parse_requirements(text)
            deps.update(parsed)
            for pkg in parsed[:20]:
                add_evidence(evidence, str(path), f"requirements:{pkg}")

        elif name == "package.json":
            pkg = parse_package_json(text)
            dep_map = {}
            dep_map.update(pkg.get("dependencies", {}) or {})
            dep_map.update(pkg.get("devDependencies", {}) or {})
            for dep in dep_map:
                deps.add(dep.lower())
                add_evidence(evidence, str(path), f"package:{dep.lower()}")

        elif name == "composer.json":
            composer = parse_composer_json(text)
            dep_map = {}
            dep_map.update(composer.get("require", {}) or {})
            dep_map.update(composer.get("require-dev", {}) or {})
            for dep in dep_map:
                deps.add(dep.lower())
                add_evidence(evidence, str(path), f"composer:{dep.lower()}")

        elif name == "pyproject.toml":
            data = parse_pyproject(text)
            project = data.get("project", {}) if isinstance(data, dict) else {}
            for dep in project.get("dependencies", []) or []:
                pkg = re.split(r"[<>=~!;\[]", dep, maxsplit=1)[0].strip().lower()
                if pkg:
                    deps.add(pkg)
                    add_evidence(evidence, str(path), f"pyproject:{pkg}")

            poetry_deps = (
                data.get("tool", {})
                .get("poetry", {})
                .get("dependencies", {})
                if isinstance(data, dict) else {}
            )
            if isinstance(poetry_deps, dict):
                for dep in poetry_deps:
                    if dep.lower() != "python":
                        deps.add(dep.lower())
                        add_evidence(evidence, str(path), f"poetry:{dep.lower()}")

        elif name == "gemfile":
            for line in text.splitlines():
                m = re.search(r"gem\s+['\"]([^'\"]+)['\"]", line)
                if m:
                    dep = m.group(1).lower()
                    deps.add(dep)
                    add_evidence(evidence, str(path), f"gem:{dep}")

        elif name == "go.mod":
            for line in text.splitlines():
                line = line.strip()
                if line.startswith("require "):
                    parts = line.split()
                    if len(parts) >= 2:
                        dep = parts[1].lower()
                        deps.add(dep)
                        add_evidence(evidence, str(path), f"go:{dep}")

    return deps


def detect_stack(root: Path) -> dict[str, Any]:
    files = find_files(root)

    scores: dict[str, dict[str, int]] = defaultdict(lambda: defaultdict(int))
    evidence: dict[str, list[str]] = defaultdict(list)

    result: dict[str, Any] = {
        "language": None,
        "framework": None,
        "template_engine": None,
        "database": [],
        "web_server": [],
        "notable_libs": [],
        "risk_indicators": [],
        "confidence": {},
        "evidence": evidence,
    }

    for path in files:
        scan_filename_hints(path, scores, evidence)

    deps = collect_dependencies(files, evidence)
    detect_from_dependency_names(deps, scores, evidence, "dependencies")

    for path in files:
        if path.suffix.lower() not in TEXT_EXTENSIONS and path.name.lower() not in {
            "dockerfile", "requirements.txt", "package.json", "composer.json",
            "pyproject.toml", "gemfile", "go.mod"
        }:
            continue

        text = safe_read_text(path)
        if not text:
            continue

        scan_source_patterns(text, path, scores, evidence)
        detect_risk_indicators_from_text(text, path, result)

    lang, lang_score = choose_best(scores["language"])
    framework, framework_score = choose_best(scores["framework"])
    template_engine, template_score = choose_best(scores["template_engine"])

    result["language"] = lang
    result["framework"] = framework
    result["template_engine"] = template_engine
    result["database"] = [
        key for key, value in sorted(scores["database"].items(), key=lambda x: x[1], reverse=True)
        if value > 0
    ]
    result["web_server"] = [
        key for key, value in sorted(scores["web_server"].items(), key=lambda x: x[1], reverse=True)
        if value > 0
    ]
    result["notable_libs"] = [
        key for key, value in sorted(scores["notable_libs"].items(), key=lambda x: x[1], reverse=True)
        if value > 0
    ]

    result["confidence"] = {
        "language": lang_score,
        "framework": framework_score,
        "template_engine": template_score,
        "database": dict(scores["database"]),
        "web_server": dict(scores["web_server"]),
        "notable_libs": dict(scores["notable_libs"]),
    }

    result["evidence"] = dict(evidence)
    return result


def main() -> int:
    root = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("challenge/source")
    output = Path(sys.argv[2]) if len(sys.argv) > 2 else Path("artifacts/stack_detect.json")

    if not root.exists():
        print(f"[!] target path not found: {root}", file=sys.stderr)
        return 1

    output.parent.mkdir(parents=True, exist_ok=True)
    result = detect_stack(root)
    output.write_text(json.dumps(result, indent=2, ensure_ascii=False), encoding="utf-8")
    print(f"[+] wrote stack detection result to {output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
