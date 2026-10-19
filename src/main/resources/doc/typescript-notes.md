# TypeScript 筆記

## 1. TypeScript 是什麼

TypeScript 是 JavaScript 的超集，主要特色是在 JavaScript 之上加入「靜態型別」。  
它最後仍然會被編譯成 JavaScript，因此可以在瀏覽器、Node.js、前端框架與後端服務中使用。

TypeScript 的核心價值：

- 在開發階段提早發現錯誤
- 讓程式碼更容易閱讀與維護
- 改善 IDE 自動補全、重構與提示
- 適合大型專案與多人協作

## 2. 基本型別

### primitive 型別

```ts
let username: string = "Lucas";
let age: number = 25;
let isAdmin: boolean = false;
let emptyValue: null = null;
let notAssigned: undefined = undefined;
```

常用型別：

- `string`：字串
- `number`：數字，包含整數與浮點數
- `boolean`：布林值
- `null`：空值
- `undefined`：尚未指定
- `symbol`：唯一識別值
- `bigint`：大整數

### array

```ts
const scores: number[] = [90, 85, 100];
const names: Array<string> = ["Amy", "Ben", "Cara"];
```

`number[]` 和 `Array<number>` 意義相同，前者較常見。

### tuple

tuple 可以限制陣列中每個位置的型別。

```ts
const user: [number, string] = [1, "Lucas"];
```

適合用在固定結構，例如座標、簡單回傳值。

```ts
const position: [number, number] = [120.5, 23.9];
```

## 3. 物件型別

```ts
const user: {
  id: number;
  name: string;
  email?: string;
} = {
  id: 1,
  name: "Lucas",
};
```

`?` 代表可選屬性。

```ts
type User = {
  id: number;
  name: string;
  email?: string;
};

const member: User = {
  id: 2,
  name: "Amy",
};
```

## 4. function 型別

### 參數與回傳值

```ts
function add(a: number, b: number): number {
  return a + b;
}
```

如果函式沒有回傳值，可以使用 `void`。

```ts
function logMessage(message: string): void {
  console.log(message);
}
```

### optional parameter

```ts
function greet(name: string, title?: string): string {
  return title ? `Hello, ${title} ${name}` : `Hello, ${name}`;
}
```

可選參數必須放在必要參數後面。

### default parameter

```ts
function createUser(name: string, role: string = "member") {
  return { name, role };
}
```

## 5. union type

union type 表示一個值可以是多種型別之一。

```ts
let id: number | string;

id = 1;
id = "user-001";
```

常用在 API 回傳、表單輸入或狀態值。

```ts
type Status = "idle" | "loading" | "success" | "error";

let status: Status = "loading";
```

這種寫法可以限制字串只能是特定值，避免拼字錯誤。

## 6. type alias

`type` 可以替型別取名字，讓型別定義更容易重複使用。

```ts
type Product = {
  id: number;
  name: string;
  price: number;
};

type ProductId = number | string;
```

`type` 也可以組合型別。

```ts
type BaseEntity = {
  id: number;
  createdAt: string;
};

type Article = BaseEntity & {
  title: string;
  content: string;
};
```

## 7. interface

`interface` 常用來描述物件結構。

```ts
interface User {
  id: number;
  name: string;
  email?: string;
}
```

也可以繼承其他 interface。

```ts
interface AdminUser extends User {
  permissions: string[];
}
```

### type 和 interface 的簡單比較

| 項目 | type | interface |
| --- | --- | --- |
| 物件型別 | 可以 | 可以 |
| union type | 可以 | 不適合 |
| intersection type | 可以 | 可用 extends |
| 宣告合併 | 不可以 | 可以 |
| 常見用途 | 彈性組合型別 | 描述物件或 class 契約 |

簡單選擇方式：

- 主要描述物件結構：可以用 `interface`
- 需要 union、tuple、複雜組合：使用 `type`

## 8. enum

enum 可以定義一組命名常數。

```ts
enum Role {
  Admin = "ADMIN",
  Member = "MEMBER",
  Guest = "GUEST",
}

const role: Role = Role.Member;
```

在現代 TypeScript 專案中，也常用 union type 取代 enum。

```ts
type Role = "ADMIN" | "MEMBER" | "GUEST";
```

union type 通常更輕量，也比較容易和字串 API 搭配。

## 9. any、unknown、never

### any

`any` 會關閉 TypeScript 的型別檢查，應該盡量避免。

```ts
let value: any = "hello";
value = 123;
value.toSomethingNotExist();
```

### unknown

`unknown` 表示目前不知道型別，但使用前必須先檢查。

```ts
function printValue(value: unknown) {
  if (typeof value === "string") {
    console.log(value.toUpperCase());
  }
}
```

比起 `any`，`unknown` 更安全。

### never

`never` 表示不可能存在的值，常見於永遠丟出錯誤或 exhaustive check。

```ts
function throwError(message: string): never {
  throw new Error(message);
}
```

## 10. type narrowing

TypeScript 會根據條件判斷縮小型別範圍。

```ts
function formatId(id: number | string): string {
  if (typeof id === "number") {
    return id.toString();
  }

  return id.toUpperCase();
}
```

常見 narrowing 方法：

- `typeof`
- `instanceof`
- `in`
- 自訂 type guard

```ts
type User = {
  id: number;
  name: string;
};

function isUser(value: unknown): value is User {
  return (
    typeof value === "object" &&
    value !== null &&
    "id" in value &&
    "name" in value
  );
}
```

## 11. generics

generics 可以讓型別像參數一樣被傳入，提高重用性。

```ts
function identity<T>(value: T): T {
  return value;
}

const text = identity<string>("hello");
const count = identity<number>(123);
```

TypeScript 通常可以自動推論 generic 型別。

```ts
const text = identity("hello");
```

### generic with array

```ts
function getFirst<T>(items: T[]): T | undefined {
  return items[0];
}

const firstName = getFirst(["Amy", "Ben"]);
const firstScore = getFirst([90, 80]);
```

### generic constraint

```ts
function getId<T extends { id: number }>(item: T): number {
  return item.id;
}
```

`extends` 在這裡代表 `T` 至少要符合某個型別結構。

## 12. utility types

TypeScript 內建許多實用型別工具。

### Partial

把所有屬性變成可選。

```ts
type User = {
  id: number;
  name: string;
  email: string;
};

type UpdateUser = Partial<User>;
```

### Pick

只挑出指定屬性。

```ts
type UserSummary = Pick<User, "id" | "name">;
```

### Omit

排除指定屬性。

```ts
type CreateUser = Omit<User, "id">;
```

### Record

建立 key-value 型別。

```ts
type Role = "admin" | "member";

const permissions: Record<Role, string[]> = {
  admin: ["read", "write", "delete"],
  member: ["read"],
};
```

### ReturnType

取得函式回傳型別。

```ts
function createUser() {
  return {
    id: 1,
    name: "Lucas",
  };
}

type CreatedUser = ReturnType<typeof createUser>;
```

## 13. class

```ts
class User {
  constructor(
    public id: number,
    public name: string,
    private password: string
  ) {}

  getDisplayName(): string {
    return `${this.id} - ${this.name}`;
  }
}
```

存取修飾子：

- `public`：外部可以存取，預設值
- `private`：只能在 class 內部使用
- `protected`：class 內部與子類別可使用
- `readonly`：初始化後不可修改

```ts
class Product {
  constructor(public readonly id: number, public name: string) {}
}
```

## 14. module import / export

### named export

```ts
export function add(a: number, b: number): number {
  return a + b;
}
```

```ts
import { add } from "./math";
```

### default export

```ts
export default function add(a: number, b: number): number {
  return a + b;
}
```

```ts
import add from "./math";
```

一般團隊中較推薦 named export，因為名稱一致性較好，也更容易重構。

## 15. async / await

```ts
type User = {
  id: number;
  name: string;
};

async function fetchUser(id: number): Promise<User> {
  const response = await fetch(`/api/users/${id}`);

  if (!response.ok) {
    throw new Error("Failed to fetch user");
  }

  return response.json();
}
```

`async` 函式的回傳型別一定是 `Promise<T>`。

## 16. DOM 與事件型別

前端開發中，事件和 DOM 元素也可以標註型別。

```ts
const button = document.querySelector<HTMLButtonElement>("#submit-button");

button?.addEventListener("click", (event: MouseEvent) => {
  console.log(event.clientX, event.clientY);
});
```

Vue、React 這類框架通常會有自己的型別輔助方式。

## 17. TypeScript 與 Vue 常見寫法

這個專案的前端使用 Vue，因此可以用以下方式搭配 TypeScript。

### script setup

```vue
<script setup lang="ts">
type User = {
  id: number;
  name: string;
};

const user: User = {
  id: 1,
  name: "Lucas",
};
</script>
```

### defineProps

```vue
<script setup lang="ts">
type Props = {
  title: string;
  count?: number;
};

const props = defineProps<Props>();
</script>
```

### defineEmits

```vue
<script setup lang="ts">
const emit = defineEmits<{
  save: [id: number];
  cancel: [];
}>();

emit("save", 1);
</script>
```

## 18. tsconfig 常見設定

`tsconfig.json` 是 TypeScript 的設定檔。

常見選項：

```json
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "ESNext",
    "strict": true,
    "moduleResolution": "Bundler",
    "skipLibCheck": true
  }
}
```

重要設定：

- `target`：編譯後要支援的 JavaScript 版本
- `module`：模組系統
- `strict`：開啟嚴格型別檢查
- `moduleResolution`：模組解析方式
- `skipLibCheck`：略過套件型別檢查，加快編譯

建議新專案盡量開啟 `strict`。

## 19. 常見錯誤與解法

### Type 'undefined' is not assignable

原因：值可能是 `undefined`，但目標型別不接受。

```ts
const names = ["Amy", "Ben"];
const firstName: string = names[0]; // 可能有風險
```

較安全的寫法：

```ts
const firstName = names[0] ?? "Unknown";
```

### Object is possibly 'null'

原因：DOM 查詢或某些 API 可能回傳 `null`。

```ts
const input = document.querySelector<HTMLInputElement>("#email");

if (input) {
  console.log(input.value);
}
```

### Property does not exist on type

原因：型別定義中沒有該屬性。

```ts
type User = {
  id: number;
  name: string;
};

const user: User = { id: 1, name: "Lucas" };
// user.email; // 錯誤，User 沒有 email
```

解法通常是：

- 確認屬性名稱是否拼錯
- 更新型別定義
- 使用 type narrowing 判斷不同型別

## 20. 實用習慣

- 優先讓 TypeScript 自動推論，不需要每個地方都手動標型別
- 函式參數與公開 API 建議明確標註型別
- 避免使用 `any`，真的不確定時先用 `unknown`
- 優先使用 union literal 限制狀態值
- API response、form data、component props 適合獨立定義型別
- 開啟 `strict` 可以讓問題更早被發現
- 不要把型別寫得比實際需求更複雜

## 21. 小型範例：API 型別設計

```ts
type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
};

type User = {
  id: number;
  name: string;
  email: string;
};

async function getUser(id: number): Promise<ApiResponse<User>> {
  const response = await fetch(`/api/users/${id}`);
  return response.json();
}
```

這種 generic response 型別很適合用在前後端分離專案，讓不同 API 可以共用同一個回應格式。

## 22. 學習順序建議

1. 先熟悉基本型別、array、object、function
2. 學會 `type`、`interface`、union type
3. 理解 type narrowing，知道如何處理不確定型別
4. 練習 generics，尤其是 API response 與共用工具函式
5. 熟悉 utility types，例如 `Partial`、`Pick`、`Omit`
6. 搭配實際框架，例如 Vue 或 React，寫 component props、emit、API 型別
7. 開啟 `strict`，養成讓型別幫你抓錯的習慣

## 23. 快速速查

```ts
type Id = number | string;

type User = {
  id: Id;
  name: string;
  email?: string;
};

type CreateUser = Omit<User, "id">;
type UpdateUser = Partial<CreateUser>;

function getDisplayName(user: User): string {
  return user.email ? `${user.name} <${user.email}>` : user.name;
}

async function request<T>(url: string): Promise<T> {
  const response = await fetch(url);

  if (!response.ok) {
    throw new Error("Request failed");
  }

  return response.json();
}
```

## 24. 總結

TypeScript 的重點不是「寫很多型別」，而是讓資料結構、函式輸入輸出、狀態變化更清楚。  
好的 TypeScript 程式碼應該讓錯誤更早出現、讓重構更安心，也讓下一個閱讀程式碼的人更快理解系統。
