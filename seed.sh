#!/bin/bash
# Заполняет БД тестовыми данными: 3 задачи + 1 контест
# Запуск: bash seed.sh

set -e
BASE="http://localhost:8080/api"
GREEN='\033[0;32m'
NC='\033[0m'

ok() { echo -e "${GREEN}✓${NC} $1"; }

echo "=== Создаём тестовые данные для Stackr ==="
echo ""

# ─── 1. Регистрируем организатора ────────────────────────────────────────────
echo "1. Регистрируем organizer..."
curl -sf -X POST "$BASE/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"username":"organizer","email":"org@stackr.io","password":"admin123"}' > /dev/null
ok "Пользователь organizer создан"

# ─── 2. Повышаем роль до ORGANIZER через БД ──────────────────────────────────
echo "2. Повышаем роль до ORGANIZER..."
docker-compose exec -T postgres \
  psql -U postgres -d stackr_db \
  -c "UPDATE users SET role='ORGANIZER' WHERE username='organizer';" > /dev/null
ok "Роль выставлена"

# ─── 3. Логинимся, получаем токен ────────────────────────────────────────────
echo "3. Получаем JWT токен..."
TOKEN=$(curl -sf -X POST "$BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"organizer","password":"admin123"}' | \
  python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")
ok "Токен получен"

AUTH="-H \"Authorization: Bearer $TOKEN\""

# ─── Вспомогательная функция ─────────────────────────────────────────────────
post() {
  curl -sf -X POST "$BASE$1" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "$2"
}

# ─── 4. Задача 1 — Easy ───────────────────────────────────────────────────────
echo "4. Создаём задачу 1 (Easy)..."
P1=$(post /problems '{
  "title": "Сумма двух чисел",
  "description": "## Задача\n\nДаны два целых числа `a` и `b`. Выведите их сумму.\n\n## Формат ввода\n\nДва целых числа на одной строке через пробел.\n\n## Формат вывода\n\nОдно целое число — сумма `a` и `b`.\n\n## Пример\n\n**Ввод:** `3 5`  \n**Вывод:** `8`",
  "difficulty": "EASY",
  "timeLimitMs": 1000,
  "memoryLimitMb": 64,
  "tagIds": [],
  "codeTemplate": "import java.util.Scanner;\n\npublic class Solution {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        int a = sc.nextInt();\n        int b = sc.nextInt();\n        // Ваш код здесь\n        System.out.println(a + b);\n    }\n}"
}' | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")

post /problems/$P1/testcases '{"inputData":"3 5","expectedOutput":"8","isPublic":true,"orderIndex":0}' > /dev/null
post /problems/$P1/testcases '{"inputData":"100 200","expectedOutput":"300","isPublic":false,"orderIndex":1}' > /dev/null
post /problems/$P1/testcases '{"inputData":"-5 10","expectedOutput":"5","isPublic":false,"orderIndex":2}' > /dev/null
post /problems/$P1/testcases '{"inputData":"0 0","expectedOutput":"0","isPublic":false,"orderIndex":3}' > /dev/null

curl -sf -X PATCH "$BASE/problems/$P1/visibility?visible=true" \
  -H "Authorization: Bearer $TOKEN" > /dev/null
ok "Задача 1 создана (id=$P1)"

# ─── 5. Задача 2 — Medium ─────────────────────────────────────────────────────
echo "5. Создаём задачу 2 (Medium)..."
P2=$(post /problems '{
  "title": "Разворот строки",
  "description": "## Задача\n\nДана строка. Выведите её символы в обратном порядке.\n\n## Формат ввода\n\nОдна строка без пробелов (длина до 100 000 символов).\n\n## Формат вывода\n\nСтрока, развёрнутая задом наперёд.\n\n## Пример\n\n**Ввод:** `hello`  \n**Вывод:** `olleh`",
  "difficulty": "MEDIUM",
  "timeLimitMs": 1000,
  "memoryLimitMb": 128,
  "tagIds": [],
  "codeTemplate": "import java.util.Scanner;\n\npublic class Solution {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        String s = sc.next();\n        // Ваш код здесь\n        System.out.println(new StringBuilder(s).reverse());\n    }\n}"
}' | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")

post /problems/$P2/testcases '{"inputData":"hello","expectedOutput":"olleh","isPublic":true,"orderIndex":0}' > /dev/null
post /problems/$P2/testcases '{"inputData":"abcde","expectedOutput":"edcba","isPublic":false,"orderIndex":1}' > /dev/null
post /problems/$P2/testcases '{"inputData":"a","expectedOutput":"a","isPublic":false,"orderIndex":2}' > /dev/null
post /problems/$P2/testcases '{"inputData":"racecar","expectedOutput":"racecar","isPublic":false,"orderIndex":3}' > /dev/null

curl -sf -X PATCH "$BASE/problems/$P2/visibility?visible=true" \
  -H "Authorization: Bearer $TOKEN" > /dev/null
ok "Задача 2 создана (id=$P2)"

# ─── 6. Задача 3 — Hard ───────────────────────────────────────────────────────
echo "6. Создаём задачу 3 (Hard)..."
P3=$(post /problems '{
  "title": "Максимальная подпоследовательность",
  "description": "## Задача\n\nДан массив целых чисел. Найдите максимальную сумму **непрерывной** подпоследовательности (алгоритм Кадане).\n\nЕсли все числа отрицательные — вернуть наибольшее из них.\n\n## Формат ввода\n\nПервая строка — число `n` (1 ≤ n ≤ 100 000).  \nВторая строка — `n` целых чисел через пробел.\n\n## Формат вывода\n\nОдно целое число — максимальная сумма подпоследовательности.\n\n## Пример\n\n**Ввод:**\n```\n5\n-2 1 -3 4 -1\n```\n**Вывод:** `4`",
  "difficulty": "HARD",
  "timeLimitMs": 2000,
  "memoryLimitMb": 256,
  "tagIds": [],
  "codeTemplate": "import java.util.Scanner;\n\npublic class Solution {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        int n = sc.nextInt();\n        long max = Long.MIN_VALUE, cur = 0;\n        for (int i = 0; i < n; i++) {\n            int x = sc.nextInt();\n            cur += x;\n            if (cur > max) max = cur;\n            if (cur < 0) cur = 0;\n        }\n        System.out.println(max);\n    }\n}"
}' | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")

post /problems/$P3/testcases '{"inputData":"5\n-2 1 -3 4 -1","expectedOutput":"4","isPublic":true,"orderIndex":0}' > /dev/null
post /problems/$P3/testcases '{"inputData":"8\n-2 -3 4 -1 -2 1 5 -3","expectedOutput":"7","isPublic":false,"orderIndex":1}' > /dev/null
post /problems/$P3/testcases '{"inputData":"1\n-5","expectedOutput":"-5","isPublic":false,"orderIndex":2}' > /dev/null
post /problems/$P3/testcases '{"inputData":"4\n1 2 3 4","expectedOutput":"10","isPublic":false,"orderIndex":3}' > /dev/null

curl -sf -X PATCH "$BASE/problems/$P3/visibility?visible=true" \
  -H "Authorization: Bearer $TOKEN" > /dev/null
ok "Задача 3 создана (id=$P3)"

# ─── 7. Контест ───────────────────────────────────────────────────────────────
echo "7. Создаём контест..."
START=$(python3 -c "
from datetime import datetime, timedelta
print((datetime.now() + timedelta(minutes=2)).strftime('%Y-%m-%dT%H:%M:%S'))
")
END=$(python3 -c "
from datetime import datetime, timedelta
print((datetime.now() + timedelta(hours=3)).strftime('%Y-%m-%dT%H:%M:%S'))
")

post /contests "{
  \"title\": \"Открытый чемпионат Stackr #1\",
  \"description\": \"Добро пожаловать на первый чемпионат платформы Stackr!\\n\\nТри задачи разного уровня сложности. Регистрируйся и покажи свои навыки в Java!\",
  \"startTime\": \"$START\",
  \"endTime\": \"$END\",
  \"problemIds\": [$P1, $P2, $P3],
  \"isPublic\": true
}" > /dev/null
ok "Контест создан (стартует через 2 минуты)"

# ─── Итог ─────────────────────────────────────────────────────────────────────
echo ""
echo "================================================"
echo -e "${GREEN}✅ Всё готово!${NC}"
echo ""
echo "  Задачи:"
echo "    • Сумма двух чисел        (Easy)"
echo "    • Разворот строки         (Medium)"
echo "    • Максимальная подпоследовательность (Hard)"
echo ""
echo "  Контест: «Открытый чемпионат Stackr #1»"
echo "    Стартует через 2 минуты, длится 3 часа"
echo ""
echo "  Войди как участник на http://localhost:5173"
echo "  Логин организатора: organizer / admin123"
echo "================================================"
