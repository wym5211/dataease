import argparse
import os
import sys
import time
import ctypes
from ctypes import wintypes
from typing import NoReturn, Optional


kernel32 = ctypes.WinDLL("kernel32", use_last_error=True)

MoveFileW = kernel32.MoveFileW
MoveFileW.argtypes = [wintypes.LPCWSTR, wintypes.LPCWSTR]
MoveFileW.restype = wintypes.BOOL

DeleteFileW = kernel32.DeleteFileW
DeleteFileW.argtypes = [wintypes.LPCWSTR]
DeleteFileW.restype = wintypes.BOOL

RemoveDirectoryW = kernel32.RemoveDirectoryW
RemoveDirectoryW.argtypes = [wintypes.LPCWSTR]
RemoveDirectoryW.restype = wintypes.BOOL


def _nt_path(path: str) -> str:
    if path.startswith("\\\\?\\") or path.startswith("\\\\.\\" ):
        return path
    return "\\\\?\\" + path


def _raise_last_error(action: str, path: str) -> NoReturn:
    code = ctypes.get_last_error()
    raise OSError(f"{action}失败：{path}") from ctypes.WinError(code)

def _absolute_path(path: str) -> str:
    if path.startswith("\\\\?\\") or path.startswith("\\\\.\\" ) or path.startswith("\\\\"):
        return path

    drive, tail = os.path.splitdrive(path)
    if drive:
        if tail.startswith("\\") or tail.startswith("/"):
            return os.path.normpath(drive + tail)
        return os.path.normpath(drive + "\\" + tail.lstrip("\\/"))

    return os.path.normpath(os.path.join(os.getcwd(), path))


def _resolve_target(target: Optional[str]) -> str:
    if not target:
        return os.path.join(os.getcwd(), "nul")

    t = target.strip()
    if "\\" not in t and "/" not in t and ":" not in t and t.rstrip(" .").lower() == "nul":
        return os.path.join(os.getcwd(), "nul")

    return _absolute_path(target)


def _is_nul_name(name: str) -> bool:
    return name.rstrip(" .").lower() == "nul"


def _find_nul_paths(root_dir: str) -> list[str]:
    root_dir = os.path.normpath(root_dir)
    matches: list[str] = []
    for current, dirnames, filenames in os.walk(root_dir, topdown=True):
        keep_dirnames: list[str] = []
        for d in dirnames:
            if _is_nul_name(d):
                matches.append(os.path.join(current, d))
            else:
                keep_dirnames.append(d)
        dirnames[:] = keep_dirnames

        for f in filenames:
            if _is_nul_name(f):
                matches.append(os.path.join(current, f))

    if _is_nul_name(os.path.basename(root_dir)):
        matches.append(root_dir)

    uniq: list[str] = []
    seen: set[str] = set()
    for p in matches:
        pn = os.path.normpath(p)
        if pn not in seen:
            seen.add(pn)
            uniq.append(pn)
    return uniq


def _delete_one_target(src_abs: str) -> int:
    src_nt = _nt_path(src_abs)

    base_dir = os.path.dirname(src_abs)
    ts = int(time.time())
    pid = os.getpid()
    dst_abs = os.path.join(base_dir, f"__nul_renamed_{ts}_{pid}.tmp")
    dst_nt = _nt_path(dst_abs)

    if not MoveFileW(src_nt, dst_nt):
        _raise_last_error("改名", src_abs)

    if DeleteFileW(dst_nt):
        print(f"已删除文件：{dst_abs}")
        return 0

    if RemoveDirectoryW(dst_nt):
        print(f"已删除目录：{dst_abs}")
        return 0

    _raise_last_error("删除", dst_abs)


def main() -> int:
    no_args = len(sys.argv) == 1
    parser = argparse.ArgumentParser(
        prog="delete_nul.py",
        description="在 Windows 上删除名为 NUL（或类似）的文件/目录：先改名再删除。无参数时默认递归搜索当前目录。",
    )
    parser.add_argument(
        "-r",
        "--recursive",
        action="store_true",
        help="递归搜索子目录中所有名为 nul 的条目并删除（无参数时默认启用）",
    )
    parser.add_argument(
        "target",
        nargs="?",
        default=None,
        help="目标路径（省略参数则递归当前目录；配合 -r 时可指定起始目录）",
    )
    args = parser.parse_args()

    if no_args:
        args.recursive = True
        args.target = None

    if args.recursive:
        root_dir = os.getcwd() if args.target is None else _absolute_path(args.target)
        if not os.path.isdir(root_dir):
            src_abs = _resolve_target(args.target)
            return _delete_one_target(src_abs)

        targets = _find_nul_paths(root_dir)
        if not targets:
            print(f"未找到：{root_dir}（及其子目录）中的 nul 条目")
            return 0

        errors = 0
        for t in targets:
            try:
                _delete_one_target(_absolute_path(t))
            except OSError as e:
                print(str(e))
                errors += 1
        return 1 if errors else 0

    src_abs = _resolve_target(args.target)
    return _delete_one_target(src_abs)


if __name__ == "__main__":
    raise SystemExit(main())
