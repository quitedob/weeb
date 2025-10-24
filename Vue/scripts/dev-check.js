#!/usr/bin/env node

/**
 * 开发环境检查脚本
 * 检查开发环境配置和规范遵循情况
 */

import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const projectRoot = path.resolve(__dirname, '..')

function checkProjectStructure() {
  console.log('🏗️  检查项目结构...')

  const requiredFiles = [
    'src/main.js',
    'src/App.vue',
    'src/assets/main.css',
    'src/assets/apple-style.css',
    'src/utils/logger.js',
    'vite.config.js',
    'package.json'
  ]

  const missingFiles = []

  requiredFiles.forEach(file => {
    const filePath = path.join(projectRoot, file)
    if (!fs.existsSync(filePath)) {
      missingFiles.push(file)
    }
  })

  if (missingFiles.length === 0) {
    console.log('✅ 项目结构检查通过')
    return true
  } else {
    console.log('❌ 缺少必要文件:')
    missingFiles.forEach(file => console.log(`   - ${file}`))
    return false
  }
}

function checkViteConfig() {
  console.log('⚙️  检查Vite配置...')

  const viteConfigPath = path.join(projectRoot, 'vite.config.js')

  if (!fs.existsSync(viteConfigPath)) {
    console.log('❌ vite.config.js 文件不存在')
    return false
  }

  const content = fs.readFileSync(viteConfigPath, 'utf8')

  // 检查关键配置
  const requiredConfigs = [
    { name: 'console清理配置', pattern: /drop_console.*mode.*===.*production/ },
    { name: '代理配置', pattern: /proxy.*api/ },
    { name: '代码分割配置', pattern: /manualChunks/ },
    { name: '别名配置', pattern: /@.*src/ }
  ]

  const missingConfigs = requiredConfigs.filter(config => !config.pattern.test(content))

  if (missingConfigs.length === 0) {
    console.log('✅ Vite配置检查通过')
    return true
  } else {
    console.log('⚠️  Vite配置可能缺少:')
    missingConfigs.forEach(config => console.log(`   - ${config.name}`))
    return false
  }
}

function checkLoggerUsage() {
  console.log('📝 检查Logger工具使用...')

  const loggerPath = path.join(projectRoot, 'src/utils/logger.js')

  if (!fs.existsSync(loggerPath)) {
    console.log('❌ logger工具不存在')
    return false
  }

  const content = fs.readFileSync(loggerPath, 'utf8')

  // 检查logger工具的完整性
  const requiredMethods = [
    'debug', 'info', 'warn', 'error', 'group', 'groupEnd', 'time', 'timeEnd'
  ]

  const missingMethods = requiredMethods.filter(method =>
    !content.includes(`${method}(`) && !content.includes(`${method}:`)
  )

  if (missingMethods.length === 0) {
    console.log('✅ Logger工具检查通过')
    return true
  } else {
    console.log('⚠️  Logger工具可能缺少方法:')
    missingMethods.forEach(method => console.log(`   - ${method}`))
    return false
  }
}

function checkPackageScripts() {
  console.log('📦 检查package.json脚本...')

  const packagePath = path.join(projectRoot, 'package.json')
  const content = JSON.parse(fs.readFileSync(packagePath, 'utf8'))

  const requiredScripts = [
    'dev', 'build', 'preview', 'clean:console', 'build:prod'
  ]

  const missingScripts = requiredScripts.filter(script =>
    !content.scripts || !content.scripts[script]
  )

  if (missingScripts.length === 0) {
    console.log('✅ package.json脚本检查通过')
    return true
  } else {
    console.log('⚠️  package.json可能缺少脚本:')
    missingScripts.forEach(script => console.log(`   - ${script}`))
    return false
  }
}

function checkStyleSystem() {
  console.log('🎨 检查样式系统...')

  const mainCssPath = path.join(projectRoot, 'src/assets/main.css')
  const appleStylePath = path.join(projectRoot, 'src/assets/apple-style.css')

  if (!fs.existsSync(mainCssPath)) {
    console.log('❌ main.css文件不存在')
    return false
  }

  if (!fs.existsSync(appleStylePath)) {
    console.log('❌ apple-style.css文件不存在')
    return false
  }

  const mainContent = fs.readFileSync(mainCssPath, 'utf8')

  // 检查是否正确导入了apple-style.css
  if (!mainContent.includes('@import') || !mainContent.includes('apple-style.css')) {
    console.log('⚠️  main.css可能没有正确导入apple-style.css')
    return false
  }

  // 检查是否包含Element Plus覆盖样式
  if (!mainContent.includes('.el-button')) {
    console.log('⚠️  main.css可能缺少Element Plus样式覆盖')
    return false
  }

  console.log('✅ 样式系统检查通过')
  return true
}

function main() {
  console.log('🚀 开始开发环境检查...\n')

  const checks = [
    { name: '项目结构', fn: checkProjectStructure },
    { name: 'Vite配置', fn: checkViteConfig },
    { name: 'Logger工具', fn: checkLoggerUsage },
    { name: 'Package脚本', fn: checkPackageScripts },
    { name: '样式系统', fn: checkStyleSystem }
  ]

  let passedChecks = 0

  checks.forEach(check => {
    console.log(`\n${'='.repeat(50)}`)
    const result = check.fn()
    if (result) {
      passedChecks++
    }
  })

  console.log(`\n${'='.repeat(50)}`)
  console.log('📊 检查结果汇总:')
  console.log(`   通过检查: ${passedChecks}/${checks.length}`)

  if (passedChecks === checks.length) {
    console.log('\n🎉 开发环境检查全部通过！可以开始开发了。')
  } else {
    console.log('\n⚠️  请修复上述问题后再开始开发。')
  }
}

// 运行检查
main()