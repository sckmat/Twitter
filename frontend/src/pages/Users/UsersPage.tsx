import { useEffect, useMemo, useState } from "react";
import styles from "./UsersPage.module.css";
import { request } from "../../api/http";
import { Button } from "../../components/ui/Button/Button";

type ApiResponse<T> = {
    code: string;
    data: T;
    message: string;
};

type User = {
    id: string;
    name: string;
    email: string;
};

export default function UsersPage() {
    const [users, setUsers] = useState<User[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [query, setQuery] = useState("");
    const [tokenClearedAt, setTokenClearedAt] = useState<number | null>(null);

    useEffect(() => {
        request<ApiResponse<User[]>>("/users")
            .then((res) => {
                const list = Array.isArray(res.data) ? res.data : [];
                setUsers(list);
            })
            .catch((e) => {
                setError(e instanceof Error ? e.message : "Ошибка загрузки пользователей");
            })
            .finally(() => setLoading(false));
    }, []);

    const filtered = useMemo(() => {
        const q = query.trim().toLowerCase();
        if (!q) return users;

        return users.filter((u) => {
            return (
                u.name.toLowerCase().includes(q) ||
                u.email.toLowerCase().includes(q)
            );
        });
    }, [users, query]);

    function onClearToken() {
        localStorage.removeItem("token");
        setTokenClearedAt(Date.now());
    }

    return (
        <div className={styles.page}>
            <div className={styles.container}>
                <div className={styles.header}>
                    <div className={styles.titleBlock}>
                        <div className={styles.titleRow}>
                            <h1 className={styles.h1}>Пользователи</h1>
                            <span className={styles.badge}>{filtered.length}</span>
                        </div>
                        <p className={styles.subtitle}>
                            Список пользователей, полученный с API
                        </p>
                    </div>

                    <div className={styles.actions}>
                        <input
                            className={styles.search}
                            placeholder="Поиск по имени или email…"
                            value={query}
                            onChange={(e) => setQuery(e.target.value)}
                        />
                        <Button type="button" variant="secondary" onClick={onClearToken}>
                            Удалить токен
                        </Button>
                    </div>
                </div>

                {tokenClearedAt ? (
                    <div className={styles.notice}>
                        Токен удалён из localStorage.
                    </div>
                ) : null}

                {loading ? (
                    <div className={styles.notice}>Загрузка пользователей...</div>
                ) : error ? (
                    <div className={styles.error}>{error}</div>
                ) : (
                    <div className={styles.card}>
                        <div className={styles.tableHeader}>
                            <div>Пользователь</div>
                            <div>Email</div>
                        </div>

                        {filtered.length === 0 ? (
                            <div className={styles.empty}>Ничего не найдено</div>
                        ) : (
                            filtered.map((u) => (
                                <div key={u.id} className={styles.row}>
                                    <div className={styles.name}>{u.name}</div>
                                    <div className={styles.email}>{u.email}</div>
                                </div>
                            ))
                        )}
                    </div>
                )}
            </div>
        </div>
    );
}
